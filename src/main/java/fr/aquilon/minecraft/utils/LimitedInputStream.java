package fr.aquilon.minecraft.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * An input stream that is set to only provide a limited number of bytes.
 * @author BilliAlpha <billi.pamege.300@gmail.com>
 */
public class LimitedInputStream extends InputStream {
    private final InputStream parent;
    private final long limit;
    private long counter;

    public LimitedInputStream(InputStream parent, long limit) {
        this.parent = Objects.requireNonNull(parent);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        if (counter+1 > limit) return -1;
        int b = parent.read();
        counter++;
        return b;
    }

    @Override
    public void close() throws IOException {
        parent.close();
    }
}
