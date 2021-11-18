/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.infrastructure.core.config;

import org.apache.fineract.infrastructure.security.filter.TenantAwareBasicAuthenticationFilter;
import org.apache.fineract.infrastructure.security.filter.TwoFactorAuthenticationFilter;
import org.apache.fineract.infrastructure.security.service.TenantAwareJpaPlatformUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;

@Configuration
@Profile("basicauth")
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private TenantAwareJpaPlatformUserDetailsService userDetailsService;

    @Autowired
    private TenantAwareBasicAuthenticationFilter tenantAwareBasicAuthenticationFilter;

    @Autowired
    private TwoFactorAuthenticationFilter twoFactorAuthenticationFilter;

    /**
     * The purpose of this method is to exclude the URL's specific to Login, Swagger UI and static files. Any URL that
     * should be excluded from the Spring security chain should be added to the ignore list in this method only
     */
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/swagger-ui/**", "/actuator/**", "/api-docs/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.antMatcher("/api/**").csrf().disable() //
                .authorizeRequests() //
                .antMatchers(HttpMethod.POST, "/api/*/echo").permitAll() //
                .antMatchers(HttpMethod.POST, "/api/*/authentication").permitAll() //
                .antMatchers(HttpMethod.POST, "/api/*/self/authentication").permitAll() //
                .antMatchers(HttpMethod.POST, "/api/*/self/registration").permitAll() //
                .antMatchers(HttpMethod.POST, "/api/*/self/registration/user").permitAll() //
                .antMatchers(HttpMethod.POST, "/api/*/twofactor/validate").fullyAuthenticated() //
                .antMatchers("/api/*/twofactor").fullyAuthenticated() //
                .antMatchers("/api/**").access("isFullyAuthenticated() and hasAuthority('TWOFACTOR_AUTHENTICATED')").and() //
                .httpBasic() //
                .authenticationEntryPoint(basicAuthenticationEntryPoint()) //
                .and() //
                .sessionManagement() //
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) //
                .and() //
                .addFilterAfter(tenantAwareBasicAuthenticationFilter, SecurityContextPersistenceFilter.class) //
                .addFilterAfter(twoFactorAuthenticationFilter, BasicAuthenticationFilter.class) //
                .requiresChannel(channel -> channel.antMatchers("/api/**").requiresSecure());

    }

    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint basicAuthenticationEntryPoint = new BasicAuthenticationEntryPoint();
        basicAuthenticationEntryPoint.setRealmName("Fineract Platform API");
        return basicAuthenticationEntryPoint;
    }

    @Bean(name = "customAuthenticationProvider")
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(authProvider());
        auth.eraseCredentials(false);
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    public FilterRegistrationBean<TenantAwareBasicAuthenticationFilter> tenantAwareBasicAuthenticationFilterRegistration() {
        FilterRegistrationBean<TenantAwareBasicAuthenticationFilter> registration = new FilterRegistrationBean<TenantAwareBasicAuthenticationFilter>(
                tenantAwareBasicAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<TwoFactorAuthenticationFilter> twoFactorAuthenticationFilterRegistration() {
        FilterRegistrationBean<TwoFactorAuthenticationFilter> registration = new FilterRegistrationBean<TwoFactorAuthenticationFilter>(
                twoFactorAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }
}
