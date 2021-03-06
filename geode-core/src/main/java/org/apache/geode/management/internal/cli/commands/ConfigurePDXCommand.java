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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import org.apache.geode.internal.cache.CacheConfig;
import org.apache.geode.internal.cache.xmlcache.CacheCreation;
import org.apache.geode.internal.cache.xmlcache.CacheXml;
import org.apache.geode.internal.cache.xmlcache.CacheXmlGenerator;
import org.apache.geode.management.cli.CliMetaData;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.InfoResultData;
import org.apache.geode.management.internal.cli.result.ResultBuilder;
import org.apache.geode.management.internal.configuration.domain.XmlEntity;
import org.apache.geode.management.internal.security.ResourceOperation;
import org.apache.geode.pdx.ReflectionBasedAutoSerializer;
import org.apache.geode.security.ResourcePermission;

public class ConfigurePDXCommand implements GfshCommand {
  @CliCommand(value = CliStrings.CONFIGURE_PDX, help = CliStrings.CONFIGURE_PDX__HELP)
  @CliMetaData(relatedTopic = CliStrings.TOPIC_GEODE_REGION)
  @ResourceOperation(resource = ResourcePermission.Resource.CLUSTER,
      operation = ResourcePermission.Operation.MANAGE)
  public Result configurePDX(
      @CliOption(key = CliStrings.CONFIGURE_PDX__READ__SERIALIZED,
          help = CliStrings.CONFIGURE_PDX__READ__SERIALIZED__HELP) Boolean readSerialized,
      @CliOption(key = CliStrings.CONFIGURE_PDX__IGNORE__UNREAD_FIELDS,
          help = CliStrings.CONFIGURE_PDX__IGNORE__UNREAD_FIELDS__HELP) Boolean ignoreUnreadFields,
      @CliOption(key = CliStrings.CONFIGURE_PDX__DISKSTORE, specifiedDefaultValue = "",
          help = CliStrings.CONFIGURE_PDX__DISKSTORE__HELP) String diskStore,
      @CliOption(key = CliStrings.CONFIGURE_PDX__AUTO__SERIALIZER__CLASSES,
          help = CliStrings.CONFIGURE_PDX__AUTO__SERIALIZER__CLASSES__HELP) String[] patterns,
      @CliOption(key = CliStrings.CONFIGURE_PDX__PORTABLE__AUTO__SERIALIZER__CLASSES,
          help = CliStrings.CONFIGURE_PDX__PORTABLE__AUTO__SERIALIZER__CLASSES__HELP) String[] portablePatterns) {
    Result result;

    try {
      InfoResultData ird = ResultBuilder.createInfoResultData();
      CacheCreation cache = new CacheCreation(true);

      if ((portablePatterns != null && portablePatterns.length > 0)
          && (patterns != null && patterns.length > 0)) {
        return ResultBuilder.createUserErrorResult(CliStrings.CONFIGURE_PDX__ERROR__MESSAGE);
      }
      if (!CliUtil.getAllNormalMembers(CliUtil.getCacheIfExists()).isEmpty()) {
        ird.addLine(CliStrings.CONFIGURE_PDX__NORMAL__MEMBERS__WARNING);
      }
      // Set persistent and the disk-store
      if (diskStore != null) {
        cache.setPdxPersistent(true);
        ird.addLine(CliStrings.CONFIGURE_PDX__PERSISTENT + " = " + cache.getPdxPersistent());
        if (!diskStore.equals("")) {
          cache.setPdxDiskStore(diskStore);
          ird.addLine(CliStrings.CONFIGURE_PDX__DISKSTORE + " = " + cache.getPdxDiskStore());
        } else {
          ird.addLine(CliStrings.CONFIGURE_PDX__DISKSTORE + " = " + "DEFAULT");
        }
      } else {
        cache.setPdxPersistent(CacheConfig.DEFAULT_PDX_PERSISTENT);
        ird.addLine(CliStrings.CONFIGURE_PDX__PERSISTENT + " = " + cache.getPdxPersistent());
      }

      // Set read-serialized
      if (readSerialized != null) {
        cache.setPdxReadSerialized(readSerialized);
      } else {
        cache.setPdxReadSerialized(CacheConfig.DEFAULT_PDX_READ_SERIALIZED);
      }
      ird.addLine(
          CliStrings.CONFIGURE_PDX__READ__SERIALIZED + " = " + cache.getPdxReadSerialized());


      // Set ignoreUnreadFields
      if (ignoreUnreadFields != null) {
        cache.setPdxIgnoreUnreadFields(ignoreUnreadFields);
      } else {
        cache.setPdxIgnoreUnreadFields(CacheConfig.DEFAULT_PDX_IGNORE_UNREAD_FIELDS);
      }
      ird.addLine(CliStrings.CONFIGURE_PDX__IGNORE__UNREAD_FIELDS + " = "
          + cache.getPdxIgnoreUnreadFields());


      if (portablePatterns != null) {
        ReflectionBasedAutoSerializer autoSerializer =
            new ReflectionBasedAutoSerializer(portablePatterns);
        cache.setPdxSerializer(autoSerializer);
        ird.addLine("PDX Serializer " + cache.getPdxSerializer().getClass().getName());
        ird.addLine("Portable classes " + Arrays.toString(portablePatterns));
      }

      if (patterns != null) {
        ReflectionBasedAutoSerializer nonPortableAutoSerializer =
            new ReflectionBasedAutoSerializer(true, patterns);
        cache.setPdxSerializer(nonPortableAutoSerializer);
        ird.addLine("PDX Serializer : " + cache.getPdxSerializer().getClass().getName());
        ird.addLine("Non portable classes :" + Arrays.toString(patterns));
      }

      final StringWriter stringWriter = new StringWriter();
      final PrintWriter printWriter = new PrintWriter(stringWriter);
      CacheXmlGenerator.generate(cache, printWriter, true, false, false);
      printWriter.close();
      String xmlDefinition = stringWriter.toString();
      // TODO jbarrett - shouldn't this use the same loadXmlDefinition that other constructors use?
      XmlEntity xmlEntity =
          XmlEntity.builder().withType(CacheXml.PDX).withConfig(xmlDefinition).build();

      result = ResultBuilder.buildResult(ird);
      persistClusterConfiguration(result,
          () -> getSharedConfiguration().addXmlEntity(xmlEntity, null));

    } catch (Exception e) {
      return ResultBuilder.createGemFireErrorResult(e.getMessage());
    }
    return result;
  }
}
