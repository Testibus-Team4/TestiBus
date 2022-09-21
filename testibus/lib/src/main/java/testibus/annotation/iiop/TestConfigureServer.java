/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package testibus.annotation.iiop;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ORBInitInfo;
import testibus.iiop.TestORBInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class TestConfigureServer {
    @ConfigureServer(
            clientOrb = @ConfigureOrb(value = "client orb", args = "base client orb"),
            serverOrb = @ConfigureOrb(value = "server orb", args = "base server orb")
    )
    abstract static class TestConfigureServerBase {
        static ORB clientOrb, serverOrb;
        static String clientOrbId, serverOrbId;

        @ConfigureServer.BeforeServer
        public static void recordServerOrb(ORB orb) {
            serverOrb = orb;
            System.out.println("### server ORB = " + serverOrb);
        }

        @BeforeAll
        public static void recordClientOrb(ORB orb) {
            clientOrb = orb;
            System.out.println("### client ORB = " + clientOrb);
        }

        @AfterAll
        public static void scrub() {
            clientOrb = serverOrb = null;
            clientOrbId = serverOrbId = null;
        }

        @ConfigureOrb.UseWithOrb("client orb")
        public static class ClientOrbInitializer implements TestORBInitializer {
            public void pre_init(ORBInitInfo info) { clientOrbId = info.arguments()[0]; }
        }

        @ConfigureOrb.UseWithOrb("server orb")
        public static class ServerOrbInitializer implements TestORBInitializer {
            public void pre_init(ORBInitInfo info) { serverOrbId = info.arguments()[0]; }
        }
    }

    @Nested
    @ConfigureServer(
            separation = ConfigureServer.Separation.INTER_ORB,
            clientOrb = @ConfigureOrb(value = "client orb", args = "inter-orb client orb"),
            serverOrb = @ConfigureOrb(value = "server orb", args = "inter-orb server orb")
    )
    class TestConfigureServerInterOrb extends TestConfigureServerBase {
        @Test
        void testServerOrbIsDistinct() {
            Assertions.assertEquals("inter-orb client orb", clientOrbId);
            Assertions.assertEquals("inter-orb server orb", serverOrbId);
            Assertions.assertNotNull(clientOrb);
            Assertions.assertNotNull(serverOrb);
            assertNotSame(clientOrb, serverOrb, "Server ORB should be distinct from client ORB");
        }
    }

    @Nested
    @ConfigureServer(
            separation = ConfigureServer.Separation.INTER_PROCESS,
            clientOrb = @ConfigureOrb(value = "client orb", args = "inter-process client orb"),
            serverOrb = @ConfigureOrb(value = "server orb", args = "inter-process server orb")
    )
    class TestConfigureServerInterProcess extends TestConfigureServerBase {
        @Test
        void testServerOrbIsNull() {
            // When we run inter process, the server-side stuff runs in the remote process, and none of the fields should be set locally
            Assertions.assertEquals("inter-process client orb", clientOrbId);
            Assertions.assertNotNull(clientOrb);
            Assertions.assertNull(serverOrbId);
            assertNull(serverOrb, "Server ORB should be unavailable in client process");
        }
    }

    @Nested
    @ConfigureServer(
            separation = ConfigureServer.Separation.COLLOCATED,
            serverOrb = @ConfigureOrb(value = "server orb", args = "collocated server orb"),
            clientOrb = @ConfigureOrb(value = "client orb", args = "collocated client orb")
    )
    class TestConfigureServerCollocated extends TestConfigureServerBase {
        @Test
        void testServerOrbIsNotDistinct() {
            Assertions.assertNull(clientOrbId); // TODO: rethink how @UseWithOrb works when the client ORB and the server ORB are the same
            Assertions.assertEquals("collocated server orb", serverOrbId);
            Assertions.assertNotNull(clientOrb);
            assertSame(clientOrb, serverOrb, "Server ORB should be identical to client ORB");
        }
    }
}
