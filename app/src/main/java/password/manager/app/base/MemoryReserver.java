/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.html.
 */

package password.manager.app.base;

import java.util.concurrent.atomic.AtomicLong;

public class MemoryReserver {

    private static final double MEM_CAP = 0.8d; // Memory cap: 80%
    private static final int INIT_BACKOFF = 4; // Initial backoff: 4 ms
    private static final int MAX_BACKOFF = 4096; // Maximum backoff: 4 s
    private static final AtomicLong reservedMemory = new AtomicLong(0);

    /**
     * Reserves the required memory, runs the given action, then releases the memory.
     * Guarantees that unlock is always called, even if the action throws.
     *
     * @param requiredMemory The amount of memory required in bytes.
     * @param action         The action to run while the memory is reserved.
     * @throws RuntimeException if interrupted while waiting for memory.
     */
    public static void execute(int requiredMemory, Runnable action) {
        lock(requiredMemory);
        try {
            action.run();
        } finally {
            unlock(requiredMemory);
        }
    }

    /**
     * Waits until sufficient memory is available for an operation.
     * Checks free memory and suggests garbage collection if needed.
     *
     * @param requiredMemory The amount of memory required in bytes.
     * @throws RuntimeException if interrupted while waiting.
     */
    private static void lock(int requiredMemory) throws RuntimeException {
        Runtime runtime = Runtime.getRuntime();
        int nextBackoff = INIT_BACKOFF;

        while (true) {
            long maxMemory = runtime.maxMemory();
            long allocatedMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long currentReserved = reservedMemory.get();
            // memory not yet allocated + free allocated memory - currently reserved memory
            long availableMemory = (maxMemory - allocatedMemory) + freeMemory - currentReserved;

            // Try to reserve the memory, if there is enough available
            if (availableMemory * MEM_CAP > requiredMemory) {
                if (reservedMemory.compareAndSet(currentReserved, currentReserved + requiredMemory)) return;
                continue; // CAS failed, another thread modified reservedMemory, retry
            }

            try {
                System.gc();
                Thread.sleep(nextBackoff);
                if (nextBackoff < MAX_BACKOFF) nextBackoff *= 2; // Exponential backoff with cap
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for memory", e);
            }
        }
    }

    /**
     * Releases reserved memory after an operation completes.
     *
     * @param requiredMemory The amount of memory to release in bytes.
     */
    private static void unlock(int requiredMemory) {
        reservedMemory.updateAndGet(current -> Math.max(0, current - requiredMemory));
    }
}
