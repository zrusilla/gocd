/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.Role;
import com.thoughtworks.go.config.RolesConfig;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.RoleConfigCreateCommand;
import com.thoughtworks.go.config.update.RoleConfigDeleteCommand;
import com.thoughtworks.go.config.update.RoleConfigUpdateCommand;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PluginRoleService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PluginRoleService.class);
    private final AuthorizationExtension authorizationExtension;
    private final GoConfigService goConfigService;
    private final EntityHashingService hashingService;

    @Autowired
    public PluginRoleService(GoConfigService goConfigService, EntityHashingService hashingService, AuthorizationExtension authorizationExtension) {
        this.goConfigService = goConfigService;
        this.hashingService = hashingService;
        this.authorizationExtension = authorizationExtension;
    }

    public Role findRole(String name) {
        return getRoles().findByName(new CaseInsensitiveString(name));
    }

    public RolesConfig listAll() {
        return getRoles();
    }

    protected void update(Username currentUser, Role role, LocalizedOperationResult result, EntityConfigUpdateCommand<Role> command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", getTagName(role), role.getName(), e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the role config. Please check the logs for more information."));
                }
            }
        }
    }

    private String getTagName(Role role) {
        return role.getClass().getAnnotation(ConfigTag.class).value();
    }

    private RolesConfig getRoles() {
        return goConfigService.serverConfig().security().getRoles();
    }

    public void update(Username currentUser, String md5, Role newRole, LocalizedOperationResult result) {
        update(currentUser, newRole, result, new RoleConfigUpdateCommand(goConfigService, newRole, authorizationExtension, currentUser, result, hashingService, md5));
    }

    public void delete(Username currentUser, Role role, LocalizedOperationResult result) {
        update(currentUser, role, result, new RoleConfigDeleteCommand(goConfigService, role, authorizationExtension, currentUser, result));
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "plugin role config", role.getName()));
        }
    }

    public void create(Username currentUser, Role newRole, LocalizedOperationResult result) {
        update(currentUser, newRole, result, new RoleConfigCreateCommand(goConfigService, newRole, authorizationExtension, currentUser, result));
    }

}
