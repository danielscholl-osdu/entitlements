/*
 * Copyright 2021 Google LLC
 * Copyright 2021 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.entitlements.v2.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.info.VersionInfoBuilder;
import org.opengroup.osdu.core.common.model.info.VersionInfo;

@RunWith(MockitoJUnitRunner.class)
public class InfoApiTest {

  @InjectMocks
  private InfoApi sut;

  @Mock
  private VersionInfoBuilder versionInfoBuilder;

  @Test
  public void should_return200_getVersionInfo() throws IOException {
    VersionInfo versionInfo = VersionInfo.builder()
        .groupId("group")
        .artifactId("artifact")
        .version("0.1.0")
        .buildTime("1000")
        .branch("master")
        .commitId("7777")
        .commitMessage("Test commit")
        .build();
    when(versionInfoBuilder.buildVersionInfo()).thenReturn(versionInfo);
    VersionInfo response = this.sut.info();
    assertEquals(versionInfo, response);
  }
}
