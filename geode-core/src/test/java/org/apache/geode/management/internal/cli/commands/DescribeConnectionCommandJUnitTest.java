/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.management.internal.cli.commands;

import org.apache.logging.log4j.Logger;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.internal.logging.LogService;
import org.apache.geode.test.junit.categories.IntegrationTest;
import org.apache.geode.test.junit.rules.GfshCommandRule;
import org.apache.geode.test.junit.rules.GfshCommandRule.PortType;
import org.apache.geode.test.junit.rules.LocatorStarterRule;

/**
 * The GfshCommandJUnitTest class is a test suite of test cases testing the contract and
 * functionality of the GfshCommand class for implementing GemFire shell (Gfsh) commands.
 *
 * @see org.apache.geode.management.internal.cli.commands.GfshCommand
 * @see org.jmock.Expectations
 * @see org.jmock.Mockery
 * @see org.jmock.lib.legacy.ClassImposteriser
 * @see org.junit.Assert
 * @see org.junit.Test
 * @since GemFire 7.0
 */

@Category(IntegrationTest.class)
public class DescribeConnectionCommandJUnitTest {
  public static Logger logger = LogService.getLogger();

  @ClassRule
  public static LocatorStarterRule locator = new LocatorStarterRule().withAutoStart();

  @Rule
  public GfshCommandRule gfsh = new GfshCommandRule();

  @Test
  public void describeConnectionTest() throws Exception {
    gfsh.connectAndVerify(locator.getJmxPort(), PortType.jmxManager);
    gfsh.executeAndAssertThat("describe connection").tableHasColumnWithValuesContaining(
        "Connection Endpoints", gfsh.getGfsh().getOperationInvoker().toString());
  }

  @Test
  public void executeWhileNotConnected() throws Exception {
    gfsh.executeAndAssertThat("describe connection")
        .tableHasColumnWithValuesContaining("Connection Endpoints", "Not connected");
  }

}
