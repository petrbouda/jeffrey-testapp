package jeffrey.testapp.server.leak;

import java.io.Closeable;
public interface NativeMemoryAllocator {

    Allocation allocate();

    class Allocation implements Closeable {

        private final long address;
        private final boolean leaked;

        public Allocation() {
            this.leaked = true;
            this.address = -1;
        }

        public Allocation(long bytes, boolean leaked) {
            this.address = UnsafeHolder.INSTANCE.allocateMemory(bytes);
            this.leaked = leaked;
        }

        @Override
        public void close() {
            if (leaked) {
                UnsafeHolder.INSTANCE.freeMemory(address);
            }
        }
    }
}
