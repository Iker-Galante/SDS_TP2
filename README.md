# TRABAJO PRACTICO NUMERO 2 SIMULACION DE SISTEMAS

Este proyecto implementa el autómata Off-Lattice de Vicsek para simular bandadas de agentes autopropulsados. Incluye el motor de simulación en Java (utilizando el método de *Cell Index Method* del TP1) y visualizaciones y análisis en Python utilizando `uv` y OVITO.

## Requisitos Previos
- **Java 17+** y **Maven** para compilar y ejecutar el motor de simulación.
- **Python 3.10+** y **uv** para manejar el entorno virtual y las dependencias de visualización (`numpy`, `matplotlib`, `ovito`, `imageio`, `imageio-ffmpeg`).

## 1. Compilación del Motor de Simulación (Java)
El código de simulación se encuentra en la carpeta raíz y reutiliza las clases del TP1 (`SDS_TP1/`). Para compilar el proyecto y generar el archivo JAR ejecutable, corre el siguiente comando en la raíz del proyecto (`SDS_TP2/`):

```bash
mvn clean package
```
Esto creará el archivo `target/vicsek-1.0-SNAPSHOT-jar-with-dependencies.jar`.

## 2. Ejecutar la Simulación Automáticamente
La forma más sencilla de correr *todo* (las simulaciones para los tres escenarios de líder, generar los gráficos y crear algunas animaciones de ejemplo) es utilizando el script automatizado:

**Linux / macOS (bash):**
```bash
./run_all.sh [directorio_salida] [densidad] [L]
```

**Windows (PowerShell):**
```powershell
.\run_all.ps1 [-OutputDir output] [-Density 4] [-L 10]
```

Por defecto, si corres el script sin argumentos, usará `output/` como directorio, densidad `4` y lado de caja `10`.

> **Nota:** Ambos scripts se encargan de llamar a `uv run python` para asegurarse de que todo corra dentro del entorno virtual de Python correctamente.

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
Para las visualizaciones, siempre debes usar el entorno virtual gestionado por `uv`. Puedes prefijar los comandos con `uv run` o activar el entorno primero.

### Gráficos de Polarización
Para generar los gráficos de evolución temporal, la polarización $v_a$ vs $\eta$, y el gráfico comparativo:

```bash
uv run python plot_polarization.py <directorio_salida> --density <densidad>
```
**Ejemplo:** `uv run python plot_polarization.py output/ --density 4.0`

### Animaciones y Video
El script `animate.py` genera cuadros PNG (con OVITO, coloreados por ángulo de velocidad y resaltando al líder) y luego los une automáticamente en un archivo `.mp4`.

```bash
uv run python animate.py <archivo_dynamic.txt> --output_dir <directorio_destino> --frames <N> --skip <salto> --fps <fps>
```

**Ejemplo básico (~1s de video):**
```bash
uv run python animate.py output/dynamic_circular_eta2.00_s0.txt --output_dir output/anim_circular --skip 5 --frames 200 --fps 30
```

#### Configurar la duración del video

La duración del video depende de cuántos cuadros se renderizan y el framerate:

**Duración (s) = (frames / skip) / fps**

| `--frames` | `--skip` | `--fps` | Cuadros renderizados | Duración |
|---|---|---|---|---|
| `200` | `5` | `30` | 40 | ~1.3s |
| `0` (todos) | `5` | `30` | ~400 | ~13s |
| `0` (todos) | `2` | `30` | ~1000 | ~33s |
| `0` (todos) | `1` | `30` | ~2001 | ~67s |

- **`--frames 0`**: procesa todos los pasos de simulación disponibles (2001 por defecto).
- **`--skip N`**: renderiza cada N-ésimo cuadro. Valores más bajos = video más largo y fluido, pero más tiempo de renderizado.
- **`--fps N`**: cuadros por segundo en el video (default: 30).
- **`--no-video`**: genera solo los cuadros PNG sin crear el `.mp4`.

**Ejemplo: video largo (~33s) con todos los pasos:**
```bash
uv run python animate.py output/dynamic_circular_eta2.00_s0.txt --output_dir output/anim_circular --skip 2 --frames 0 --fps 30
```

> **Nota:** Cada cuadro requiere un render de OVITO, por lo que usar `--skip 1` con muchos frames puede tardar varios minutos. Un buen balance es `--skip 2` o `--skip 3`.