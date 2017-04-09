package edu.ufl.cise.cnt5106c; /**
 * Created by Jiya on 3/30/17.
 */

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

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
                throw new UnsupportedOperationException ("Message not supported.");
        }
}
