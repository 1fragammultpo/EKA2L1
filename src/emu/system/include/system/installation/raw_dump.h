/*
 * Copyright (c) 2020 EKA2L1 Team
 *
 * This file is part of EKA2L1 project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

#pragma once

#include <system/installation/common.h>
#include <common/types.h>

#include <atomic>
#include <cstdint>
#include <string>

namespace eka2l1 {
    class device_manager;
}

namespace eka2l1::loader {
    device_installation_error install_raw_dump(device_manager *dvc, const std::string &path_to_dump, const std::string &devices_z_path, std::string &firmware_code,
        std::atomic<int> &res);
}