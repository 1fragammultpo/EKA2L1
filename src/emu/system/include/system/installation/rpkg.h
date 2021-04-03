#pragma once

#include <system/installation/common.h>
#include <common/types.h>

#include <atomic>
#include <cstdint>
#include <string>

namespace eka2l1 {
    class io_system;
    class device_manager;

    namespace loader {
        struct rpkg_header {
            std::uint32_t magic[4];
            std::uint8_t major_rom;
            std::uint8_t minor_rom;
            std::uint16_t build_rom;
            std::uint32_t count;
            std::uint32_t header_size;
            std::uint32_t machine_uid;
        };

        struct rpkg_entry {
            std::uint64_t attrib;
            std::uint64_t time;
            std::uint64_t path_len;

            std::u16string path;

            std::uint64_t data_size;
        };

        device_installation_error install_rpkg(device_manager *dvc, const std::string &path, const std::string &devices_rom_path, std::string &firmware_code, std::atomic<int> &res);
    }
}