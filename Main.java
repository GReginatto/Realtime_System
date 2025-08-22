import javax.realtime.*;

public class Main {

    public static void main(String[] args) {
        
        // --- 1. Criar o objeto compartilhado que representa a caldeira ---
        BoilerSystem boiler = new BoilerSystem();

        // --- 2. Configurar os parâmetros de tempo real para as threads ---

        // Scheduler: Usaremos o scheduler de prioridade fixa padrão
        PriorityScheduler scheduler = (PriorityScheduler) Scheduler.getDefaultScheduler();

        // Prioridades: O controlador deve ter prioridade maior que a simulação
        int controlPriority = scheduler.getMaxPriority() - 10;
        int simulationPriority = controlPriority - 10;
        
        // Parâmetros de release para a SIMULAÇÃO (período de 1 segundo)
        RelativeTime simulationPeriod = new RelativeTime(1000, 0); // 1000ms
        PeriodicParameters simReleaseParams = new PeriodicParameters(
            null,                // Start time (null = start now)
            simulationPeriod,    // Period
            null,                // Cost (ignored by JamaicaVM)
            null,                // Deadline (null = same as period)
            null,                // Overrun handler
            null                 // Miss handler
        );
        
        // Parâmetros de release para o CONTROLE (período de 5 segundos)
        RelativeTime controlPeriod = new RelativeTime(5000, 0); // 5000ms
        PeriodicParameters ctrlReleaseParams = new PeriodicParameters(
            null,
            controlPeriod,
            null,
            null,
            null,
            null
        );

        // 3. Criar e iniciar as Threads de Tempo Real

        System.out.println("Iniciando o sistema de controle da caldeira...");

        BoilerSimulation simulationThread = new BoilerSimulation(
            new PriorityParameters(simulationPriority), 
            simReleaseParams, 
            boiler
        );

        BoilerController controllerThread = new BoilerController(
            new PriorityParameters(controlPriority), 
            ctrlReleaseParams, 
            boiler
        );

        // Inicia as threads
        simulationThread.start();
        controllerThread.start();

        // 4. Simular Eventos Externos (falhas)
        // O código a seguir pode ser usado para injetar falhas e testar
        // as transições de modo do sistema.
        try {
            // Deixa o sistema rodar normalmente por 30 segundos
            Thread.sleep(30000); 
            
            // Simula uma falha na bomba 1 
            boiler.setPumpFailed(0, true);
            
            // Deixa rodar em modo degradado por 30 segundos
            Thread.sleep(30000);
            
            // Simula uma falha no sensor de nível de água 
            boiler.setWaterLevelSensorFailed(true);

            // Aguarda o controlador detectar e ir para parada de emergência
             Thread.sleep(10000);

            // Simula o reparo da bomba 1 e do sensor
            boiler.setPumpFailed(0, false);
            boiler.setWaterLevelSensorFailed(false);
            // NOTA: O sistema não sairá do modo EMERGENCY_STOP sozinho.
            // Seria necessário um comando manual para reiniciar (ir para INITIALIZATION).
            System.out.println("Fim da simulação de falhas.");


        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Em um sistema real, o programa principal poderia esperar indefinidamente
        // ou aguardar um comando de desligamento.
        try {
            controllerThread.join();
            simulationThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}