#include "memory.h"

#include <cstdint>
#include <fstream>
#include <string>
#include <cstdio>

namespace {

uint64_t readMemInfo(const char* key) {
    std::ifstream file("/proc/meminfo");

    if (!file.is_open())
        return 0;

    std::string line;

    while (std::getline(file, line)) {
        if (line.rfind(key, 0) == 0) {
            unsigned long long kb = 0;

            if (sscanf(line.c_str() + std::strlen(key),
                       "%llu",
                       &kb) == 1) {
                return kb * 1024ULL;
            }
        }
    }

    return 0;
}

}

namespace WinlatorMemory {

uint64_t getVirtualTotalMemory() {
    return VIRTUAL_RAM_12GB;
}

uint64_t getVirtualAvailableMemory() {
    uint64_t realAvailable =
            readMemInfo("MemAvailable:");

    if (realAvailable == 0)
        return 0;

    /*
     * Limita a memória virtual disponível
     * ao máximo de 12 GiB.
     */
    return realAvailable > VIRTUAL_RAM_12GB
            ? VIRTUAL_RAM_12GB
            : realAvailable;
}

uint64_t getVirtualUsedMemory() {
    uint64_t available =
            getVirtualAvailableMemory();

    if (available >= VIRTUAL_RAM_12GB)
        return 0;

    return VIRTUAL_RAM_12GB - available;
}

}
