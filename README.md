# TRABAJO PRACTICO NUMERO 2 SIMULACION DE SISTEMAS

Este proyecto implementa el autómata Off-Lattice de Vicsek para simular bandadas de agentes autopropulsados. Incluye el motor de simulación en Java (utilizando el método de *Cell Index Method* del TP1) y visualizaciones y análisis en Python utilizando `uv` y OVITO.

## Requisitos Previos
- **Java 17+** y **Maven** para compilar y ejecutar el motor de simulación.
- **Python 3.10+** y **uv** para manejar el entorno virtual y las dependencias de visualización (`numpy`, `matplotlib`, `ovito`).

## 1. Compilación del Motor de Simulación (Java)
El código de simulación se encuentra en la carpeta raíz y reutiliza las clases del TP1 (`SDS_TP1/`). Para compilar el proyecto y generar el archivo JAR ejecutable, corre el siguiente comando en la raíz del proyecto (`SDS_TP2/`):

```bash
mvn clean package
```
Esto creará el archivo `target/vicsek-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## 2. Ejecutar la Simulación Automáticamente
La forma más sencilla de correr *todo* (las simulaciones para los tres escenarios de líder, generar los gráficos y crear algunas animaciones de ejemplo) es utilizando el script automatizado:

```bash
./run_all.sh [directorio_salida] [densidad] [L]
```
Por defecto, si corres `./run_all.sh` sin argumentos, usará `output/` como directorio, densidad `4` y lado de caja `10`.
> **Nota:** El script se encarga de llamar a `uv run python` para asegurarse de que todo corra dentro del entorno virtual de Python correctamente.

## 3. Ejecutar las Simulaciones Manualmente (Batch Runner)
Si deseas correr los barridos de ruido ($\eta$) generados para un escenario específico, puedes utilizar el `BatchRunner`. Los escenarios disponibles son: `none` (Sin líder), `fixed` (Líder dirección fija), `circular` (Líder circular).

```bash
java -cp target/vicsek-1.0-SNAPSHOT-jar-with-dependencies.jar ar.edu.itba.ss.vicsek.BatchRunner <escenario> <directorio_salida> <densidad> <L>
```

**Ejemplo:**
```bash
java -cp target/vicsek-1.0-SNAPSHOT-jar-with-dependencies.jar ar.edu.itba.ss.vicsek.BatchRunner circular output 4 10
```
Esto correrá 5 semillas para cada valor de ruido $\eta \in [0, 5]$, generando archivos de posiciones y un `.csv` de resumen en la carpeta `output/`.

## 4. Visualización y Análisis (Python)
Para las visualizaciones, siempre debes usar el entorno virtual gestionado por `uv`. Puedes prefijar los comandos con `uv run` o activar el entorno primero (`source .venv/bin/activate`).

### Gráficos de Polarización
Para generar los gráficos de evolución temporal, la polarización $v_a$ vs $\eta$, y el gráfico comparativo:

```bash
uv run python plot_polarization.py <directorio_salida> --density <densidad>
```
**Ejemplo:** `uv run python plot_polarization.py output/ --density 4.0`

### Animaciones (Video MP4) en OVITO
Para generar directamente un video `.mp4` con la animación (usando *colormaps* HSV según el ángulo de orientación para las velocidades, y resaltando a la partícula líder en blanco/rojo):

```bash
uv run python animate_video.py <archivo_dynamic.txt> <archivo_video.mp4> --frames <N> --skip <salto>
```
**Ejemplo:**
```bash
uv run python animate_video.py output/dynamic_circular_eta2.00_s0.txt output/circular_leader.mp4 --skip 5 --frames 1000
```
Esto procesará las posiciones e internamente utilizará el motor de OVITO para exportar y guardar automáticamente el video resultante de la simulación.