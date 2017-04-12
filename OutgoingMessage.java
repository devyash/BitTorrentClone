
/*
This class extends the inbuilt DataOutputStream Class and implements the ObjectOutput interface
*A data output stream lets an application write primitive java data Types
* ObjectOutput is used for writing objects of type object, strings and arrays.
 *  */
import java.io.*;

public class OutgoingMessage extends DataOutputStream implements ObjectOutput {

        public OutgoingMessage(OutputStream out) {
            super(out);
        }

        @Override
        public void writeObject (Object o) throws IOException {
            if (o instanceof HandShakeMessage) {
                ((HandShakeMessage) o).write(this);
            }
            else if (o instanceof ActualMessage) {
                ((ActualMessage) o).write(this);
            }
            else
                throw new UnsupportedOperationException ("Please provide either handshake message or Actual Message type");
        }
}
