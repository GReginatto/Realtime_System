/**
 * Enumeração para os diferentes modos de operação da caldeira,
 */
public enum OperatingMode {
    /**
     * Modo inicial onde as verificações são realizadas para decidir o próximo estado. 
     */
    INITIALIZATION,
    /**
     * Operação normal, mantendo o nível de água entre os limites normais.
     */
    NORMAL,
    /**
     * Operação com falha em algum dispositivo, tentando manter o nível satisfatório. 
     */
    DEGRADED,
    /**
     * Operação quando o sensor de nível de água falha. 
     */
    RESCUE,
    /**
     * Parada total do sistema devido a falhas críticas ou níveis de água fora dos limites. 
     */
    EMERGENCY_STOP
}