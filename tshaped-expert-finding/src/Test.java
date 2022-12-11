public class Test {
    public static void main(String[] args) {
        double[] y = new double[]{1.0, 0.0, 0.0, 0.0, 0.0};
        double[] p_ca_1 = new double[]{0.6, 0.1, 0.1, 0.1, 0.1};
        double[] p_ca_2 = new double[]{0.6, 0.4, 0.0, 0.0, 0.0};

        double entropy_ca_1 = 0.0, entropy_ca_2 = 0.0;
        for (double p : p_ca_1) {
            if (p == 0.0)
                continue;
            entropy_ca_1 += -p * ((Math.log10(p)) / (Math.log10(2)));
        }
        for (double p : p_ca_2) {
            if (p == 0.0)
                continue;
            entropy_ca_2 += -p * ((Math.log10(p)) / (Math.log10(2)));
        }
        System.out.println("entropy_ca_1:" + entropy_ca_1 + " entropy_ca_2:" + entropy_ca_2);

        double bce_ca_1 = 0.0, bce_ca_2 = 0.0;
        for (int i = 0; i < p_ca_1.length; i++) {
            double prob = p_ca_1[i], y_i = y[i];
            if (prob == 0.0)
                prob = 1e-10;
            if (y_i == 1.0) {
                if (prob != 0.0)
                    bce_ca_1 += -(Math.log10(prob) / Math.log10(2)); // -log_2(q_i)
                else
                    bce_ca_1 += -(Math.log10(1e-10) / Math.log10(2)); // -log_2(q_i)
            } else {
                if (prob != 1.0)
                    bce_ca_1 += -(Math.log10(1 - prob) / Math.log10(2)); // -log_2(1-q_i)
                else
                    bce_ca_1 += -(Math.log10(1e-10) / Math.log10(2)); // -log_2(1-q_i)
            }
        }
        for (int i = 0; i < p_ca_2.length; i++) {
            double prob = p_ca_2[i], y_i = y[i];
            if (prob == 0.0)
                prob = 1e-10;
            if (y_i == 1.0) {
                if (prob != 0.0)
                    bce_ca_2 += -(Math.log10(prob) / Math.log10(2)); // -log_2(q_i)
                else
                    bce_ca_2 += -(Math.log10(1e-10) / Math.log10(2)); // -log_2(q_i)
            } else {
                if (prob != 1.0)
                    bce_ca_2 += -(Math.log10(1 - prob) / Math.log10(2)); // -log_2(1-q_i)
                else
                    bce_ca_2 += -(Math.log10(1e-10) / Math.log10(2)); // -log_2(1-q_i)
            }
        }
        bce_ca_1 = bce_ca_1 / p_ca_1.length;
        bce_ca_2 = bce_ca_2 / p_ca_2.length;
        System.out.println("ace_ca_1:" + bce_ca_1 + " ace_ca_2:" + bce_ca_2);

        double wbce_ca_1 = 0.0, wbce_ca_2 = 0.0;
        for (int i = 0; i < p_ca_1.length; i++) {
            double prob = p_ca_1[i], y_i = y[i];
            if (prob == 0.0)
                prob = 1e-10;
            if (y_i == 1.0) {
                if (prob != 0.0)
                    wbce_ca_1 += -0.5 * (Math.log10(prob) / Math.log10(2)); // -0.5 * log_2(q_i)
                else
                    wbce_ca_1 += -0.5 * (Math.log10(1e-10) / Math.log10(2)); // -0.5 * log_2(q_i)
            } else {
                if (prob != 1.0)
                    wbce_ca_1 += -0.5 * (Math.log10(1 - prob) / Math.log10(2)) / (p_ca_1.length - 1); // -log_2(1-q_i)
                else
                    wbce_ca_1 += -0.5 * (Math.log10(1e-10) / Math.log10(2)) / (p_ca_1.length - 1); // -log_2(1-q_i)
            }
        }
        for (int i = 0; i < p_ca_2.length; i++) {
            double prob = p_ca_2[i], y_i = y[i];
            if (prob == 0.0)
                prob = 1e-10;
            if (y_i == 1.0) {
                if (prob != 0.0)
                    wbce_ca_2 += -0.5 * (Math.log10(prob) / Math.log10(2)); // -0.5 * log_2(q_i)
                else
                    wbce_ca_2 += -0.5 * (Math.log10(1e-10) / Math.log10(2)); // -0.5 * log_2(q_i)
            } else {
                if (prob != 1.0)
                    wbce_ca_2 += -0.5 * (Math.log10(1 - prob) / Math.log10(2)) / (p_ca_2.length - 1); // -log_2(1-q_i)
                else
                    wbce_ca_2 += -0.5 * (Math.log10(1e-10) / Math.log10(2)) / (p_ca_2.length - 1); // -log_2(1-q_i)
            }
        }
        System.out.println("wce_ca_1:" + wbce_ca_1 + " wce_ca_2:" + wbce_ca_2);

        double wfl_ca_1 = 0.0, wfl_ca_2 = 0.0;
        for (int i = 0; i < p_ca_1.length; i++) {
            double prob = p_ca_1[i], y_i = y[i];
            if (prob == 0.0)
                prob = 1e-10;
            if (y_i == 1.0) {
                if (prob != 0.0)
                    wfl_ca_1 += -0.5 * Math.pow(1 - prob, 2) * (Math.log10(prob) / Math.log10(2)); // (1-q_i)^2*log_2(q_i)
                else
                    wfl_ca_1 += -0.5 * Math.pow(1 - 1e-10, 2) * (Math.log10(1e-10) / Math.log10(2)); // (1-q_i)^2*log_2(q_i)
            } else {
                if (prob != 1.0)
                    wfl_ca_1 += -0.5 * Math.pow(prob, 2) * (Math.log10(1 - prob) / Math.log10(2)) / (p_ca_1.length - 1); // q_i^2*log_2(1-q_i)
                else
                    wfl_ca_1 += -0.5 * Math.pow(1 - 1e-10, 2) * (Math.log10(1e-10) / Math.log10(2)) / (p_ca_1.length - 1); // q_i^2*log_2(1-q_i)
            }
        }
        for (int i = 0; i < p_ca_2.length; i++) {
            double prob = p_ca_2[i], y_i = y[i];
            if (prob == 0.0)
                prob = 1e-10;
            if (y_i == 1.0) {
                if (prob != 0.0)
                    wfl_ca_2 += -0.5 * Math.pow(1 - prob, 2) * (Math.log10(prob) / Math.log10(2)); // (1-q_i)^2*log_2(q_i)
                else
                    wfl_ca_2 += -0.5 * Math.pow(1 - 1e-10, 2) * (Math.log10(1e-10) / Math.log10(2)); // (1-q_i)^2*log_2(q_i)
            } else {
                if (prob != 1.0)
                    wfl_ca_2 += -0.5 * Math.pow(prob, 2) * (Math.log10(1 - prob) / Math.log10(2)) / (p_ca_2.length - 1); // q_i^2*log_2(1-q_i)
                else
                    wfl_ca_2 += -0.5 * Math.pow(1 - 1e-10, 2) * (Math.log10(1e-10) / Math.log10(2)) / (p_ca_2.length - 1); // q_i^2*log_2(1-q_i)
            }
        }
        System.out.println("wfl_ca_1:" + wfl_ca_1 + " wfl_ca_2:" + wfl_ca_2);

    }
}
