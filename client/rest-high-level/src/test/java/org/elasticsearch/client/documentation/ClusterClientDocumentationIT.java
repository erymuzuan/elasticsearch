/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.client.documentation;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.LatchedActionListener;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.ingest.GetPipelineRequest;
import org.elasticsearch.action.ingest.GetPipelineResponse;
import org.elasticsearch.action.ingest.PutPipelineRequest;
import org.elasticsearch.action.ingest.DeletePipelineRequest;
import org.elasticsearch.action.ingest.WritePipelineResponse;
import org.elasticsearch.client.ESRestHighLevelClientTestCase;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.cluster.routing.allocation.decider.EnableAllocationDecider;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.indices.recovery.RecoverySettings;
import org.elasticsearch.ingest.PipelineConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.equalTo;

/**
 * This class is used to generate the Java Cluster API documentation.
 * You need to wrap your code between two tags like:
 * // tag::example
 * // end::example
 *
 * Where example is your tag name.
 *
 * Then in the documentation, you can extract what is between tag and end tags with
 * ["source","java",subs="attributes,callouts,macros"]
 * --------------------------------------------------
 * include-tagged::{doc-tests}/ClusterClientDocumentationIT.java[example]
 * --------------------------------------------------
 *
 * The column width of the code block is 84. If the code contains a line longer
 * than 84, the line will be cut and a horizontal scroll bar will be displayed.
 * (the code indentation of the tag is not included in the width)
 */
public class ClusterClientDocumentationIT extends ESRestHighLevelClientTestCase {

    public void testClusterPutSettings() throws IOException {
        RestHighLevelClient client = highLevelClient();

        // tag::put-settings-request
        ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();
        // end::put-settings-request

        // tag::put-settings-create-settings
        String transientSettingKey =
                RecoverySettings.INDICES_RECOVERY_MAX_BYTES_PER_SEC_SETTING.getKey();
        int transientSettingValue = 10;
        Settings transientSettings =
                Settings.builder()
                .put(transientSettingKey, transientSettingValue, ByteSizeUnit.BYTES)
                .build(); // <1>

        String persistentSettingKey =
                EnableAllocationDecider.CLUSTER_ROUTING_ALLOCATION_ENABLE_SETTING.getKey();
        String persistentSettingValue =
                EnableAllocationDecider.Allocation.NONE.name();
        Settings persistentSettings =
                Settings.builder()
                .put(persistentSettingKey, persistentSettingValue)
                .build(); // <2>
        // end::put-settings-create-settings

        // tag::put-settings-request-cluster-settings
        request.transientSettings(transientSettings); // <1>
        request.persistentSettings(persistentSettings); // <2>
        // end::put-settings-request-cluster-settings

        {
            // tag::put-settings-settings-builder
            Settings.Builder transientSettingsBuilder =
                    Settings.builder()
                    .put(transientSettingKey, transientSettingValue, ByteSizeUnit.BYTES);
            request.transientSettings(transientSettingsBuilder); // <1>
            // end::put-settings-settings-builder
        }
        {
            // tag::put-settings-settings-map
            Map<String, Object> map = new HashMap<>();
            map.put(transientSettingKey
                    , transientSettingValue + ByteSizeUnit.BYTES.getSuffix());
            request.transientSettings(map); // <1>
            // end::put-settings-settings-map
        }
        {
            // tag::put-settings-settings-source
            request.transientSettings(
                    "{\"indices.recovery.max_bytes_per_sec\": \"10b\"}"
                    , XContentType.JSON); // <1>
            // end::put-settings-settings-source
        }

        // tag::put-settings-request-timeout
        request.timeout(TimeValue.timeValueMinutes(2)); // <1>
        request.timeout("2m"); // <2>
        // end::put-settings-request-timeout
        // tag::put-settings-request-masterTimeout
        request.masterNodeTimeout(TimeValue.timeValueMinutes(1)); // <1>
        request.masterNodeTimeout("1m"); // <2>
        // end::put-settings-request-masterTimeout

        // tag::put-settings-execute
        ClusterUpdateSettingsResponse response = client.cluster().putSettings(request);
        // end::put-settings-execute

        // tag::put-settings-response
        boolean acknowledged = response.isAcknowledged(); // <1>
        Settings transientSettingsResponse = response.getTransientSettings(); // <2>
        Settings persistentSettingsResponse = response.getPersistentSettings(); // <3>
        // end::put-settings-response
        assertTrue(acknowledged);
        assertThat(transientSettingsResponse.get(transientSettingKey), equalTo(transientSettingValue + ByteSizeUnit.BYTES.getSuffix()));
        assertThat(persistentSettingsResponse.get(persistentSettingKey), equalTo(persistentSettingValue));

        // tag::put-settings-request-reset-transient
        request.transientSettings(Settings.builder().putNull(transientSettingKey).build()); // <1>
        // tag::put-settings-request-reset-transient
        request.persistentSettings(Settings.builder().putNull(persistentSettingKey));
        ClusterUpdateSettingsResponse resetResponse = client.cluster().putSettings(request);

        assertTrue(resetResponse.isAcknowledged());
    }

    public void testClusterUpdateSettingsAsync() throws Exception {
        RestHighLevelClient client = highLevelClient();
        {
            ClusterUpdateSettingsRequest request = new ClusterUpdateSettingsRequest();

            // tag::put-settings-execute-listener
            ActionListener<ClusterUpdateSettingsResponse> listener =
                    new ActionListener<ClusterUpdateSettingsResponse>() {
                @Override
                public void onResponse(ClusterUpdateSettingsResponse response) {
                    // <1>
                }

                @Override
                public void onFailure(Exception e) {
                    // <2>
                }
            };
            // end::put-settings-execute-listener

            // Replace the empty listener by a blocking listener in test
            final CountDownLatch latch = new CountDownLatch(1);
            listener = new LatchedActionListener<>(listener, latch);

            // tag::put-settings-execute-async
            client.cluster().putSettingsAsync(request, listener); // <1>
            // end::put-settings-execute-async

            assertTrue(latch.await(30L, TimeUnit.SECONDS));
        }
    }

    public void testPutPipeline() throws IOException {
        RestHighLevelClient client = highLevelClient();

        {
            // tag::put-pipeline-request
            String source =
                "{\"description\":\"my set of processors\"," +
                    "\"processors\":[{\"set\":{\"field\":\"foo\",\"value\":\"bar\"}}]}";
            PutPipelineRequest request = new PutPipelineRequest(
                "my-pipeline-id", // <1>
                new BytesArray(source.getBytes(StandardCharsets.UTF_8)), // <2>
                XContentType.JSON // <3>
            );
            // end::put-pipeline-request

            // tag::put-pipeline-request-timeout
            request.timeout(TimeValue.timeValueMinutes(2)); // <1>
            request.timeout("2m"); // <2>
            // end::put-pipeline-request-timeout

            // tag::put-pipeline-request-masterTimeout
            request.masterNodeTimeout(TimeValue.timeValueMinutes(1)); // <1>
            request.masterNodeTimeout("1m"); // <2>
            // end::put-pipeline-request-masterTimeout

            // tag::put-pipeline-execute
            WritePipelineResponse response = client.cluster().putPipeline(request); // <1>
            // end::put-pipeline-execute

            // tag::put-pipeline-response
            boolean acknowledged = response.isAcknowledged(); // <1>
            // end::put-pipeline-response
            assertTrue(acknowledged);
        }
    }

    public void testPutPipelineAsync() throws Exception {
        RestHighLevelClient client = highLevelClient();

        {
            String source =
                "{\"description\":\"my set of processors\"," +
                    "\"processors\":[{\"set\":{\"field\":\"foo\",\"value\":\"bar\"}}]}";
            PutPipelineRequest request = new PutPipelineRequest(
                "my-pipeline-id",
                new BytesArray(source.getBytes(StandardCharsets.UTF_8)),
                XContentType.JSON
            );

            // tag::put-pipeline-execute-listener
            ActionListener<WritePipelineResponse> listener =
                new ActionListener<WritePipelineResponse>() {
                    @Override
                    public void onResponse(WritePipelineResponse response) {
                        // <1>
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // <2>
                    }
                };
            // end::put-pipeline-execute-listener

            // Replace the empty listener by a blocking listener in test
            final CountDownLatch latch = new CountDownLatch(1);
            listener = new LatchedActionListener<>(listener, latch);

            // tag::put-pipeline-execute-async
            client.cluster().putPipelineAsync(request, listener); // <1>
            // end::put-pipeline-execute-async

            assertTrue(latch.await(30L, TimeUnit.SECONDS));
        }
    }

    public void testGetPipeline() throws IOException {
        RestHighLevelClient client = highLevelClient();

        {
            createPipeline("my-pipeline-id");
        }

        {
            // tag::get-pipeline-request
            GetPipelineRequest request = new GetPipelineRequest("my-pipeline-id"); // <1>
            // end::get-pipeline-request

            // tag::get-pipeline-request-masterTimeout
            request.masterNodeTimeout(TimeValue.timeValueMinutes(1)); // <1>
            request.masterNodeTimeout("1m"); // <2>
            // end::get-pipeline-request-masterTimeout

            // tag::get-pipeline-execute
            GetPipelineResponse response = client.cluster().getPipeline(request); // <1>
            // end::get-pipeline-execute

            // tag::get-pipeline-response
            boolean successful = response.isFound(); // <1>
            List<PipelineConfiguration> pipelines = response.pipelines(); // <2>
            for(PipelineConfiguration pipeline: pipelines) {
                Map<String, Object> config = pipeline.getConfigAsMap(); // <3>
            }
            // end::get-pipeline-response

            assertTrue(successful);
        }
    }

    public void testGetPipelineAsync() throws Exception {
        RestHighLevelClient client = highLevelClient();

        {
            createPipeline("my-pipeline-id");
        }

        {
            GetPipelineRequest request = new GetPipelineRequest("my-pipeline-id");

            // tag::get-pipeline-execute-listener
            ActionListener<GetPipelineResponse> listener =
                new ActionListener<GetPipelineResponse>() {
                    @Override
                    public void onResponse(GetPipelineResponse response) {
                        // <1>
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // <2>
                    }
                };
            // end::get-pipeline-execute-listener

            // Replace the empty listener by a blocking listener in test
            final CountDownLatch latch = new CountDownLatch(1);
            listener = new LatchedActionListener<>(listener, latch);

            // tag::get-pipeline-execute-async
            client.cluster().getPipelineAsync(request, listener); // <1>
            // end::get-pipeline-execute-async

            assertTrue(latch.await(30L, TimeUnit.SECONDS));
        }
    }

    public void testDeletePipeline() throws IOException {
        RestHighLevelClient client = highLevelClient();

        {
            createPipeline("my-pipeline-id");
        }

        {
            // tag::delete-pipeline-request
            DeletePipelineRequest request = new DeletePipelineRequest("my-pipeline-id"); // <1>
            // end::delete-pipeline-request

            // tag::delete-pipeline-request-timeout
            request.timeout(TimeValue.timeValueMinutes(2)); // <1>
            request.timeout("2m"); // <2>
            // end::delete-pipeline-request-timeout

            // tag::delete-pipeline-request-masterTimeout
            request.masterNodeTimeout(TimeValue.timeValueMinutes(1)); // <1>
            request.masterNodeTimeout("1m"); // <2>
            // end::delete-pipeline-request-masterTimeout

            // tag::delete-pipeline-execute
            WritePipelineResponse response = client.cluster().deletePipeline(request); // <1>
            // end::delete-pipeline-execute

            // tag::delete-pipeline-response
            boolean acknowledged = response.isAcknowledged(); // <1>
            // end::delete-pipeline-response
            assertTrue(acknowledged);
        }
    }

    public void testDeletePipelineAsync() throws Exception {
        RestHighLevelClient client = highLevelClient();

        {
            createPipeline("my-pipeline-id");
        }

        {
            DeletePipelineRequest request = new DeletePipelineRequest("my-pipeline-id");

            // tag::delete-pipeline-execute-listener
            ActionListener<WritePipelineResponse> listener =
                new ActionListener<WritePipelineResponse>() {
                    @Override
                    public void onResponse(WritePipelineResponse response) {
                        // <1>
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // <2>
                    }
                };
            // end::delete-pipeline-execute-listener

            // Replace the empty listener by a blocking listener in test
            final CountDownLatch latch = new CountDownLatch(1);
            listener = new LatchedActionListener<>(listener, latch);

            // tag::delete-pipeline-execute-async
            client.cluster().deletePipelineAsync(request, listener); // <1>
            // end::delete-pipeline-execute-async

            assertTrue(latch.await(30L, TimeUnit.SECONDS));
        }
    }
}
