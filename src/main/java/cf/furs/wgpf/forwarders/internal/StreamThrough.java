package cf.furs.wgpf.forwarders.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface StreamThrough {
     public void forwardStream(InputStream in, OutputStream out) throws IOException;
}
