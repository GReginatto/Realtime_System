import javax.realtime.*;

/**
 * Thread de tempo real que controla a caldeira.
 * Executa a cada 5 segundos para analisar os dados e tomar decisões. 
 */
public class BoilerController extends RealtimeThread {

    private final BoilerSystem boilerSystem;

    public BoilerController(SchedulingParameters scheduling, ReleaseParameters release, BoilerSystem system) {
        super(scheduling, release);
        this.boilerSystem = system;
    }

    @Override
    public void run() {
        System.out.println("Controlador iniciado.");
        while (true) {
            // Espera pelo próximo período de 5 segundos
            waitForNextPeriod();

            // 1. Recepção das mensagens (Leitura dos sensores) 
            double waterLevel = boilerSystem.getWaterLevel();
            double steamOutput = boilerSystem.getSteamOutput();
            OperatingMode currentMode = boilerSystem.getCurrentMode();

            // 2. Análise das informações e tomada de decisão 
            // A lógica é uma máquina de estados baseada no modo de operação.
            switch (currentMode) {
                case INITIALIZATION:
                    handleInitialization(waterLevel, steamOutput);
                    break;
                case NORMAL:
                    handleNormal(waterLevel);
                    break;
                case DEGRADED:
                    handleDegraded(waterLevel);
                    break;
                case RESCUE:
                    handleRescue();
                    break;
                case EMERGENCY_STOP:
                    // Uma vez em parada de emergência, não faz mais nada.
                    break;
            }

            // Imprime um status a cada ciclo de controle
            printStatus(waterLevel, currentMode);
        }
    }
    
    private void handleInitialization(double waterLevel, double steamOutput) {
        // Verifica todos os componentes 
        if (Double.isNaN(waterLevel) || Double.isNaN(steamOutput) || boilerSystem.isPumpFailed(0) || boilerSystem.isPumpFailed(1)) {
            // Se algum sensor crítico ou bomba já começa com falha, vai para o modo apropriado.
            if(Double.isNaN(waterLevel)) {
                boilerSystem.setCurrentMode(OperatingMode.RESCUE);
            } else {
                boilerSystem.setCurrentMode(OperatingMode.DEGRADED);
            }
        } else if (waterLevel < BoilerSystem.MIN_LIMIT || waterLevel > BoilerSystem.MAX_LIMIT) {
            // Se o nível inicial já está fora dos limites de segurança.
            boilerSystem.setCurrentMode(OperatingMode.EMERGENCY_STOP);
            shutdownAllPumps();
        } else {
            // Tudo OK, vai para o modo normal.
            boilerSystem.setCurrentMode(OperatingMode.NORMAL);
        }
    }

    private void handleNormal(double waterLevel) {
        // Verifica por novas falhas
        if (Double.isNaN(waterLevel)) {
            boilerSystem.setCurrentMode(OperatingMode.RESCUE);
            return;
        }
        if (boilerSystem.isPumpFailed(0) || boilerSystem.isPumpFailed(1) || Double.isNaN(boilerSystem.getSteamOutput())) {
            boilerSystem.setCurrentMode(OperatingMode.DEGRADED);
            return;
        }

        // Verifica limites de segurança
        if (waterLevel < BoilerSystem.MIN_LIMIT || waterLevel > BoilerSystem.MAX_LIMIT) {
            boilerSystem.setCurrentMode(OperatingMode.EMERGENCY_STOP);
            shutdownAllPumps();
            return;
        }

        // Lógica de controle principal: manter nível entre MIN_NORMAL e MAX_NORMAL
        if (waterLevel < BoilerSystem.MIN_NORMAL) {
            // Nível baixo, liga uma bomba se possível
            turnOnOnePump();
        } else if (waterLevel > BoilerSystem.MAX_NORMAL) {
            // Nível alto, desliga uma bomba
            turnOffOnePump();
        }
    }
    
    private void handleDegraded(double waterLevel) {
        // Verifica se o problema foi resolvido
        if (!Double.isNaN(waterLevel) && !Double.isNaN(boilerSystem.getSteamOutput()) && !boilerSystem.isPumpFailed(0) && !boilerSystem.isPumpFailed(1)) {
            System.out.println(">>> Todos os sistemas reparados. Retornando ao modo NORMAL.");
            boilerSystem.setCurrentMode(OperatingMode.NORMAL);
            return;
        }

        // Verifica se a situação piorou
        if (Double.isNaN(waterLevel)) {
            boilerSystem.setCurrentMode(OperatingMode.RESCUE);
            return;
        }
        if (boilerSystem.isPumpFailed(0) && boilerSystem.isPumpFailed(1)) {
             System.err.println("!!! FALHA CRÍTICA: Ambas as bombas falharam!");
             boilerSystem.setCurrentMode(OperatingMode.EMERGENCY_STOP);
             shutdownAllPumps();
             return;
        }

        // Lógica de controle degradado: faz o melhor possível com os recursos restantes
        if (waterLevel < BoilerSystem.MIN_LIMIT || waterLevel > BoilerSystem.MAX_LIMIT) {
            boilerSystem.setCurrentMode(OperatingMode.EMERGENCY_STOP);
            shutdownAllPumps();
            return;
        }
        
        if (waterLevel < BoilerSystem.MIN_NORMAL) {
            turnOnOnePump(); // Tenta ligar a bomba que ainda funciona
        } else if (waterLevel > BoilerSystem.MAX_NORMAL) {
            turnOffOnePump(); // Tenta desligar uma bomba
        }
    }
    
    private void handleRescue() {
        // ligar uma bomba por um tempo e depois desligar, tentando manter um equilíbrio.
        // Uma abordagem mais segura seria ir para parada de emergência.
        // Vamos sinalizar a emergência, pois estimar sem feedback é perigoso.
        System.err.println("MODO RESGATE: Sensor de nível de água inoperante. Impossível garantir operação segura.");
        boilerSystem.setCurrentMode(OperatingMode.EMERGENCY_STOP);
        shutdownAllPumps();
    }


    //Métodos Auxiliares de Controle de Bombas (Ações Físicas) 

    private void turnOnOnePump() {
        // Tenta ligar a primeira bomba que estiver desligada e funcionando
        for (int i = 0; i < BoilerSystem.NUM_PUMPS; i++) {
            if (!boilerSystem.isPumpOn(i) && !boilerSystem.isPumpFailed(i)) {
                boilerSystem.setPumpOn(i, true);
                return; // Liga apenas uma por ciclo de controle
            }
        }
    }

    private void turnOffOnePump() {
        // Tenta desligar a primeira bomba que estiver ligada
        for (int i = 0; i < BoilerSystem.NUM_PUMPS; i++) {
            if (boilerSystem.isPumpOn(i)) {
                boilerSystem.setPumpOn(i, false);
                return; // Desliga apenas uma por ciclo
            }
        }
    }
    
    private void shutdownAllPumps() {
        System.err.println("!!! COMANDO: Desligando todas as bombas!");
        for (int i = 0; i < BoilerSystem.NUM_PUMPS; i++) {
            boilerSystem.setPumpOn(i, false);
        }
    }

    private void printStatus(double waterLevel, OperatingMode currentMode) {
        String pumpStatus = "";
        for (int i = 0; i < BoilerSystem.NUM_PUMPS; i++) {
            pumpStatus += "Bomba " + (i + 1) + ": " + (boilerSystem.isPumpOn(i) ? "ON" : "OFF") + 
                          (boilerSystem.isPumpFailed(i) ? " (FALHA)" : "") + " | ";
        }
        System.out.printf("[Controle] Modo: %-15s | Nível Água: %-7.2f L | %s\n", 
            currentMode, waterLevel, pumpStatus);
    }
}