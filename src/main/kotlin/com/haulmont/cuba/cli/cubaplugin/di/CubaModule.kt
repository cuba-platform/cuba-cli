/*
 * Copyright (c) 2008-2018 Haulmont.
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

package com.haulmont.cuba.cli.cubaplugin.di

import com.haulmont.cuba.cli.cubaplugin.ProjectService
import com.haulmont.cuba.cli.cubaplugin.ProjectServiceImpl
import com.haulmont.cuba.cli.cubaplugin.gradle.GradleRunner
import com.haulmont.cuba.cli.cubaplugin.model.EntitySearch
import com.haulmont.cuba.cli.cubaplugin.prefixchange.PrefixChanger
import com.haulmont.cli.core.kodein
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersionsManager
import com.haulmont.cuba.cli.cubaplugin.model.PlatformVersionsManagerImpl
import com.haulmont.cuba.cli.registration.EntityRegistrationHelper
import com.haulmont.cuba.cli.registration.ScreenRegistrationHelper
import com.haulmont.cuba.cli.registration.ServiceRegistrationHelper
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton

private val cubaModule = Kodein.Module {
    bind<ScreenRegistrationHelper>() with singleton {
        ScreenRegistrationHelper()
    }

    bind<ServiceRegistrationHelper>() with singleton {
        ServiceRegistrationHelper()
    }

    bind<EntityRegistrationHelper>() with singleton {
        EntityRegistrationHelper()
    }

    bind<PrefixChanger>() with singleton {
        PrefixChanger()
    }

    bind<EntitySearch>() with singleton {
        EntitySearch()
    }

    bind<GradleRunner>() with singleton {
        GradleRunner()
    }

    bind<ProjectService>() with singleton {
        ProjectServiceImpl()
    }

    bind<PlatformVersionsManager>() with singleton {
        PlatformVersionsManagerImpl()
    }
}

internal val cubaKodein = Kodein {
    extend(kodein)

    import(cubaModule)
}
