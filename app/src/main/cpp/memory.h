#ifndef WINLATOR_MEMORY_H
#define WINLATOR_MEMORY_H

#include <cstdint>

namespace WinlatorMemory {

constexpr uint64_t VIRTUAL_RAM_12GB =
        12ULL * 1024ULL * 1024ULL * 1024ULL;

uint64_t getVirtualTotalMemory();
uint64_t getVirtualAvailableMemory();
uint64_t getVirtualUsedMemory();

}

#endif
