/**
 * Representa o sistema físico da caldeira e seus componentes.
 */
public class BoilerSystem {

    // Parâmetros Físicos da Caldeira (conforme especificação) 
    public static final double MAX_CAPACITY = 1000.0;     // C: Capacidade máxima em litros 
    public static final double MIN_LIMIT = 150.0;         // M1: Limite mínimo de água 
    public static final double MAX_LIMIT = 850.0;         // M2: Limite máximo de água 
    public static final double MIN_NORMAL = 400.0;        // N1: Nível normal mínimo 
    public static final double MAX_NORMAL = 600.0;        // N2: Nível normal máximo
    public static final double STEAM_OUTPUT_RATE = 70.0;  // V: Vazão de vapor em litros/segundo 
    public static final double PUMP_CAPACITY = 50.0;      // P: Capacidade de cada bomba em litros/segundo 
    public static final int NUM_PUMPS = 2;                // Simplificação do trabalho: usar 2 bombas 

    // Variáveis de Estado do Sistema 
    private double waterLevel;                          // q: Quantidade atual de água
    private OperatingMode currentMode;                  // Modo de operação atual
    
    // Estado dos componentes
    private final boolean[] pumpOn;                     // Controla se uma bomba está ligada ou desligada
    private final boolean[] pumpFailed;                 // Simula a falha de uma bomba 
    private boolean waterLevelSensorFailed;             // Simula a falha do sensor de nível 
    private boolean steamSensorFailed;                  // Simula a falha do sensor de vapor 

    /**
     * Construtor do sistema da caldeira.
     * Inicia com um nível de água médio e no modo de inicialização.
     */
    public BoilerSystem() {
        // Começa com o nível de água no meio da faixa normal
        this.waterLevel = (MIN_NORMAL + MAX_NORMAL) / 2.0;
        this.currentMode = OperatingMode.INITIALIZATION;
        
        this.pumpOn = new boolean[NUM_PUMPS];
        this.pumpFailed = new boolean[NUM_PUMPS];
        
        for (int i = 0; i < NUM_PUMPS; i++) {
            this.pumpOn[i] = false;
            this.pumpFailed[i] = false;
        }
        this.pumpOn[0] = true;
        this.waterLevelSensorFailed = false;
        this.steamSensorFailed = false;
    }

    // Métodos sincronizados

    public synchronized double getWaterLevel() {
        if (waterLevelSensorFailed) {
            // Se o sensor falhou, o controlador não tem uma leitura confiável.
            return Double.NaN; 
        }
        return waterLevel;
    }

    public synchronized void setWaterLevel(double level) {
        // Garante que o nível da água fique dentro da capacidade da caldeira
        if (level < 0) {
            this.waterLevel = 0;
        } else if (level > MAX_CAPACITY) {
            this.waterLevel = MAX_CAPACITY;
        } else {
            this.waterLevel = level;
        }
    }

    public synchronized double getSteamOutput() {
        if (steamSensorFailed) {
            return Double.NaN; // Indica falha na leitura
        }
        return STEAM_OUTPUT_RATE;
    }
    
    public synchronized boolean isPumpOn(int pumpIndex) {
        return pumpOn[pumpIndex];
    }
    
    public synchronized void setPumpOn(int pumpIndex, boolean status) {
        if (pumpIndex < NUM_PUMPS && !pumpFailed[pumpIndex]) {
            this.pumpOn[pumpIndex] = status;
        }
    }

    public synchronized boolean isPumpFailed(int pumpIndex) {
        return pumpFailed[pumpIndex];
    }

    public synchronized OperatingMode getCurrentMode() {
        return currentMode;
    }

    public synchronized void setCurrentMode(OperatingMode mode) {
        if (this.currentMode != mode) {
            System.out.println(">>> MUDANÇA DE MODO: " + this.currentMode + " -> " + mode);
            this.currentMode = mode;
        }
    }

    //Métodos para Simular Falhas

    public synchronized void setPumpFailed(int pumpIndex, boolean failed) {
        this.pumpFailed[pumpIndex] = failed;
        if (failed) {
            this.pumpOn[pumpIndex] = false; // Se a bomba falha, ela desliga.
            System.err.println("FALHA DETECTADA: Bomba " + (pumpIndex + 1) + " falhou.");
        } else {
             System.out.println("REPARO: Bomba " + (pumpIndex + 1) + " reparada.");
        }
    }

    public synchronized void setWaterLevelSensorFailed(boolean failed) {
        this.waterLevelSensorFailed = failed;
        if(failed) System.err.println("FALHA DETECTADA: Sensor de nível de água falhou.");
        else System.out.println("REPARO: Sensor de nível de água reparado.");
    }
    
    public synchronized void setSteamSensorFailed(boolean failed) {
        this.steamSensorFailed = failed;
         if(failed) System.err.println("FALHA DETECTADA: Sensor de vapor falhou.");
         else System.out.println("REPARO: Sensor de vapor reparado.");
    }
}