/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.messaging.remote.internal.hub

import org.gradle.messaging.dispatch.MethodInvocation
import org.gradle.messaging.dispatch.StreamCompletion
import org.gradle.messaging.remote.internal.ConnectCompletion
import org.gradle.messaging.remote.internal.TestConnection
import org.gradle.messaging.remote.internal.hub.protocol.ChannelIdentifier
import org.gradle.messaging.remote.internal.hub.protocol.ChannelMessage
import org.gradle.test.fixtures.concurrent.ConcurrentSpec

class MessageHubBackedObjectConnectionTest extends ConcurrentSpec {
    def connectCompletion = Mock(ConnectCompletion)
    def connectionBuilder = new MessageHubBackedObjectConnection(executorFactory, connectCompletion)

    interface Worker {
        void doStuff(String value)
    }

    interface CompletableWorker extends Worker, StreamCompletion {
    }

    def "forwards incoming method invocations to handler"() {
        def worker = Mock(Worker)
        def connection = new TestConnection()

        given:
        connectCompletion.create(_) >> connection
        connection.queueIncoming(new ChannelMessage(new ChannelIdentifier(Worker.name), new MethodInvocation(Worker.class.getMethod("doStuff", String), ["param 1"] as Object[])))

        when:
        connectionBuilder.addIncoming(Worker, worker)
        connectionBuilder.connect()
        connection.stop()
        connectionBuilder.stop()

        then:
        1 * worker.doStuff("param 1")
        0 * worker._

        cleanup:
        connection?.stop()
        connectionBuilder?.stop()
    }

    def "notifies handler of end of incoming messages when it implements StreamCompletion"() {
        def worker = Mock(CompletableWorker)
        def connection = new TestConnection()

        given:
        connectCompletion.create(_) >> connection
        connection.queueIncoming(new ChannelMessage(new ChannelIdentifier(Worker.name), new MethodInvocation(Worker.class.getMethod("doStuff", String), ["param 1"] as Object[])))

        when:
        connectionBuilder.addIncoming(Worker, worker)
        connectionBuilder.connect()
        connection.stop()
        connectionBuilder.stop()

        then:
        1 * worker.doStuff("param 1")
        1 * worker.endStream()
        0 * worker._

        cleanup:
        connection?.stop()
        connectionBuilder?.stop()
    }
}