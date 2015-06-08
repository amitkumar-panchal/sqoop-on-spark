/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sqoop.job.spark;

import org.apache.log4j.Logger;
import org.apache.sqoop.common.Direction;
import org.apache.sqoop.connector.matcher.Matcher;
import org.apache.sqoop.connector.matcher.MatcherFactory;
import org.apache.sqoop.core.JobConstants;
import org.apache.sqoop.driver.JobRequest;
import org.apache.sqoop.job.SparkPrefixContext;
import org.apache.sqoop.job.etl.Destroyer;
import org.apache.sqoop.job.etl.DestroyerContext;
import org.apache.sqoop.schema.Schema;
import org.apache.sqoop.utils.ClassUtils;

/**
 * Helper class to execute destroyers on spark job side.
 */
public class SparkDestroyerUtil {

  public static final Logger LOG = Logger.getLogger(SparkDestroyerUtil.class);

  private SparkDestroyerUtil() {
  }

  public static void executeDestroyer(boolean success, JobRequest request, Direction direction) {
    String destroyerPropertyName, prefixPropertyName;
    switch (direction) {
    default:
    case FROM:
      destroyerPropertyName = JobConstants.JOB_ETL_FROM_DESTROYER;
      prefixPropertyName = JobConstants.PREFIX_CONNECTOR_FROM_CONTEXT;
      break;

    case TO:
      destroyerPropertyName = JobConstants.JOB_ETL_TO_DESTROYER;
      prefixPropertyName = JobConstants.PREFIX_CONNECTOR_TO_CONTEXT;
      break;
    }

    Destroyer destroyer = (Destroyer) ClassUtils.instantiate(request.getDriverContext().getString(
        destroyerPropertyName));

    if (destroyer == null) {
      LOG.info("Skipping running destroyer as non was defined.");
      return;
    }

    // Objects that should be pass to the Destroyer execution
    SparkPrefixContext subContext = new SparkPrefixContext(request.getConf(), prefixPropertyName);
    Object configConnection = request.getConnectorLinkConfig(direction);
    Object configJob = request.getJobConfig(direction);

    Schema fromSchema = request.getJobSubmission().getFromSchema();
    Schema toSchema = request.getJobSubmission().getToSchema();
    Matcher matcher = MatcherFactory.getMatcher(fromSchema, toSchema);

    Schema schema = (direction == Direction.FROM ? matcher.getFromSchema() : matcher.getToSchema());

    DestroyerContext destroyerContext = new DestroyerContext(subContext, success, schema);

    LOG.info("Executing destroyer class " + destroyer.getClass());
    destroyer.destroy(destroyerContext, configConnection, configJob);
  }

}
