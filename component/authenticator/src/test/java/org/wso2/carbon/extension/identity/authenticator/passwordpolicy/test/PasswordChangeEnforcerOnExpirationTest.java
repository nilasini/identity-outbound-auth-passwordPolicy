/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.extension.identity.authenticator.passwordpolicy.test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.powermock.reflect.Whitebox;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.application.authentication.framework.ApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.AuthenticatorFlowStatus;

import org.wso2.carbon.identity.application.authentication.framework.LocalApplicationAuthenticator;
import org.wso2.carbon.identity.application.authentication.framework.config.ConfigurationFacade;
import org.wso2.carbon.identity.application.authentication.framework.config.model.AuthenticatorConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.SequenceConfig;
import org.wso2.carbon.identity.application.authentication.framework.config.model.StepConfig;
import org.wso2.carbon.identity.application.authentication.framework.context.AuthenticationContext;
import org.wso2.carbon.identity.application.authentication.framework.exception.AuthenticationFailedException;

import org.wso2.carbon.identity.application.authentication.framework.exception.LogoutFailedException;
import org.wso2.carbon.identity.application.authentication.framework.model.AuthenticatedUser;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkUtils;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.policy.password.PasswordChangeEnforceConstants;
import org.wso2.carbon.identity.policy.password.PasswordChangeEnforcerOnExpiration;
import org.wso2.carbon.identity.policy.password.PasswordChangeUtils;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({IdentityTenantUtil.class, ConfigurationFacade.class, FrameworkUtils.class})
public class PasswordChangeEnforcerOnExpirationTest {
    private PasswordChangeEnforcerOnExpiration passwordChangeEnforcerOnExpiration;

    @Mock
    private ConfigurationFacade configurationFacade;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Spy
    private AuthenticationContext context;

    @Mock
    private SequenceConfig sequenceConfig;

    @Mock
    private Map<Integer, StepConfig> mockedMap;

    @Mock
    private StepConfig stepConfig;

    @Spy
    private PasswordChangeEnforcerOnExpiration spy;

    @Mock
    private AuthenticatorConfig authenticatorConfig;

    @Mock
    private ApplicationAuthenticator applicationAuthenticator;

    @Mock
    private LocalApplicationAuthenticator localApplicationAuthenticator;

    @Mock
    private UserStoreManager userStoreManager;

    @Mock
    private UserRealm userRealm;

    @Mock
    private RealmService realmService;

    @BeforeMethod
    public void setUp() throws Exception {
        passwordChangeEnforcerOnExpiration = new PasswordChangeEnforcerOnExpiration();
        initMocks(this);
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Test
    public void testCanHandleTrue() {
        Assert.assertEquals(passwordChangeEnforcerOnExpiration.canHandle(httpServletRequest), true);
    }

    @Test
    public void testGetFriendlyName() {
        Assert.assertEquals(passwordChangeEnforcerOnExpiration.getFriendlyName(),
                PasswordChangeEnforceConstants.AUTHENTICATOR_NAME);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(passwordChangeEnforcerOnExpiration.getName(),
                PasswordChangeEnforceConstants.AUTHENTICATOR_NAME);
    }

    @Test
    public void testGetContextIdentifier() {
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.STATE)).thenReturn("abc");
        Assert.assertEquals(passwordChangeEnforcerOnExpiration.getContextIdentifier(httpServletRequest),
                "abc");
    }


    @Test
    public void testGetUsername() throws Exception {
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "getUsername", context);
    }

    @Test
    public void testUpdateAuthenticatedUserInStepConfig() throws Exception {
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "updateAuthenticatedUserInStepConfig",
                context, user);
    }

    @Test
    public void testProcessWithLogoutTrue() throws AuthenticationFailedException, LogoutFailedException {
        when(context.isLogoutRequest()).thenReturn(true);
        AuthenticatorFlowStatus status = passwordChangeEnforcerOnExpiration.process(httpServletRequest,
                httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test
    public void testProcessWithLogoutFailure() throws Exception {
        when(context.isLogoutRequest()).thenReturn(false);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.CURRENT_PWD)).thenReturn("1234");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD)).thenReturn("7894");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD_CONFIRMATION)).thenReturn("7894");
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(authenticatorConfig);
        when(authenticatorConfig.getApplicationAuthenticator()).thenReturn(applicationAuthenticator);
        when(Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "getUsername", context)).
                thenReturn(user);
        AuthenticatorFlowStatus status = passwordChangeEnforcerOnExpiration.process(httpServletRequest,
                httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessWithLogout() throws Exception {
        when(context.isLogoutRequest()).thenReturn(false);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.CURRENT_PWD)).thenReturn("");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD)).thenReturn("");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD_CONFIRMATION)).thenReturn("");
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(authenticatorConfig);
        when(authenticatorConfig.getApplicationAuthenticator()).thenReturn(applicationAuthenticator);
        AuthenticatorFlowStatus status = passwordChangeEnforcerOnExpiration.process(httpServletRequest,
                httpServletResponse, context);
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testInitiateAuthRequestWithException() throws Exception {
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(authenticatorConfig);
        when(authenticatorConfig.getApplicationAuthenticator()).thenReturn(applicationAuthenticator);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        when(Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "initiateAuthRequest", httpServletRequest,
                httpServletResponse, context, ""))
                .thenReturn(user);

    }

    @Test
    public void testInitiateAuthRequest() throws Exception {
        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(authenticatorConfig);
        when(authenticatorConfig.getApplicationAuthenticator()).thenReturn(applicationAuthenticator);
        when(stepConfig.getAuthenticatedUser()).thenReturn(user);
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "initiateAuthRequest", httpServletRequest,
                httpServletResponse, context, "");
        AuthenticatorFlowStatus status = AuthenticatorFlowStatus.SUCCESS_COMPLETED;
        Assert.assertEquals(status, AuthenticatorFlowStatus.SUCCESS_COMPLETED);
    }

    @Test
    public void testInitiateAuthRequestSuccess() throws Exception {
        mockStatic(IdentityTenantUtil.class);
        mockStatic(ConfigurationFacade.class);
        mockStatic(FrameworkUtils.class);

        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedAutenticator()).thenReturn(authenticatorConfig);
        when(authenticatorConfig.getApplicationAuthenticator()).thenReturn(localApplicationAuthenticator);
        when(stepConfig.getAuthenticatedUser()).thenReturn(user);
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(userRealm.getUserStoreManager()
                .getUserClaimValue(MultitenantUtils.getTenantAwareUsername("admin"),
                        PasswordChangeUtils.LAST_PASSWORD_CHANGED_TIMESTAMP_CLAIM, null)).thenReturn("1461315067665");
        when(ConfigurationFacade.getInstance()).thenReturn(configurationFacade);
        when(configurationFacade.getAuthenticationEndpointURL()).thenReturn("login.do");
        when(FrameworkUtils.getQueryStringWithFrameworkContextId(context.getQueryParams(),
                context.getCallerSessionKey(), context.getContextIdentifier())).thenReturn(null);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "initiateAuthRequest", httpServletRequest,
                httpServletResponse, context, "");
        verify(httpServletResponse).sendRedirect(captor.capture());
        Assert.assertTrue(captor.getValue().contains(PasswordChangeEnforceConstants.AUTHENTICATOR_NAME));
    }

    @Test
    public void testProcessAuthenticationResponse() throws Exception {
        mockStatic(IdentityTenantUtil.class);

        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedUser()).thenReturn(user);
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.CURRENT_PWD)).thenReturn("123456");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD)).thenReturn("456789");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD_CONFIRMATION)).thenReturn("456789");
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessAuthenticationResponseWithException() throws Exception {
        mockStatic(IdentityTenantUtil.class);

        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedUser()).thenReturn(user);
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.CURRENT_PWD)).thenReturn(null);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD)).thenReturn(null);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD_CONFIRMATION)).thenReturn(null);
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessResponseWithSamePassword() throws Exception {
        mockStatic(IdentityTenantUtil.class);

        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedUser()).thenReturn(user);
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.CURRENT_PWD)).thenReturn("12345");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD)).thenReturn("12345");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD_CONFIRMATION)).thenReturn("12345");
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @Test(expectedExceptions = {AuthenticationFailedException.class})
    public void testProcessResponseWithMismatchPassword() throws Exception {
        mockStatic(IdentityTenantUtil.class);

        when(context.getSequenceConfig()).thenReturn(sequenceConfig);
        when(sequenceConfig.getStepMap()).thenReturn(mockedMap);
        when(mockedMap.get(anyObject())).thenReturn(stepConfig);
        AuthenticatedUser user = AuthenticatedUser.createLocalAuthenticatedUserFromSubjectIdentifier("admin");
        when(stepConfig.getAuthenticatedUser()).thenReturn(user);
        when(IdentityTenantUtil.getTenantId("carbon.super")).thenReturn(-1234);
        when(IdentityTenantUtil.getRealmService()).thenReturn(realmService);
        when(realmService.getTenantUserRealm(-1234)).thenReturn(userRealm);
        when(userRealm.getUserStoreManager()).thenReturn(userStoreManager);
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.CURRENT_PWD)).thenReturn("12345");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD)).thenReturn("456789");
        when(httpServletRequest.getParameter(PasswordChangeEnforceConstants.NEW_PWD_CONFIRMATION)).thenReturn("12345");
        Whitebox.invokeMethod(passwordChangeEnforcerOnExpiration, "processAuthenticationResponse",
                httpServletRequest, httpServletResponse, context);
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }
}
