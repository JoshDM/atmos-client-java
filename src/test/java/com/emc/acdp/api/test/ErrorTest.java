/*
 * Copyright (c) 2013-2016, EMC Corporation.
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * + Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * + Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * + The name of EMC Corporation may not be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.emc.acdp.api.test;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.emc.acdp.AcdpException;
import com.emc.acdp.api.jersey.AcdpAdminApiClient;

public class ErrorTest {
    private static final String DO_NOT_CREATE_THIS_ACCOUNT = "delete_this_account_immediately";

    AcdpAdminApiClient admin;

    @Before
    public void setUp() throws Exception {
        try {
            admin = new AcdpAdminApiClient( AcdpTestUtil.loadAdminConfig() );
        } catch(Exception e) {
            Assume.assumeNoException("Loading acdp.properties failed", e);
        }

    }

    @Test
    public void testErrorParsing() {
        try {
            admin.getAccount( DO_NOT_CREATE_THIS_ACCOUNT );
            Assert.fail( "Test account should not exist, but does!" );
        } catch ( AcdpException e ) {
            Assert.assertNotNull( "ACDP code is null", e.getAcdpCode() );
        }
    }

}
