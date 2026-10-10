🛰️ Signal Radar - Navegación Táctica 3D (Android / Kotlin)
Aplicación Android de brújula y navegación táctica de alta precisión con renderizado 3D, integración de sensores de presión atmosférica y gestión avanzada de waypoints. Diseñada bajo una estética oscura cyberpunk.
<p align="center">
<img src="[https://img.shields.io/badge/Status-Open%20Source-00FF66?style=for-the-badge&logo=github](https://img.shields.io/badge/Status-Open%20Source-00FF66?style=for-the-badge&logo=github)" alt="Open Source">
<img src="[https://img.shields.io/badge/Kotlin-Android-purple?style=for-the-badge&logo=kotlin](https://img.shields.io/badge/Kotlin-Android-purple?style=for-the-badge&logo=kotlin)" alt="Kotlin">
<img src="[https://img.shields.io/badge/License-MIT-blue?style=for-the-badge](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)" alt="License">
</p>
📌 ¿Qué es Signal Radar?
Signal Radar es una herramienta de orientación y seguimiento de campo desarrollada en Kotlin para Android. Está pensada para situaciones donde la precisión milimétrica y la lectura rápida de datos (como altitud barométrica y rumbo exacto) son vitales.
La aplicación elimina interfaces saturadas o técnicas confusas, ofreciendo un entorno visual limpio, minimalista y completamente en español.
🚀 Características Principales
Proyección 3D de Waypoints (Pin con Mástil Vertical): Los puntos de referencia no se muestran planos; se proyectan en un espacio tridimensional con un pin elevado que facilita su ubicación visual rápida.
Sensor Barométrico Nativo (Sensor.TYPE_PRESSURE): Captura de la altitud sobre el nivel del mar en tiempo real, reflejando variaciones de altura al instante de forma mucho más sensible que el GPS convencional.
Control de Zoom por Gestos (ScaleGestureDetector): Adiós a los botones estorbosos en pantalla. El zoom se realiza directamente pellizcando el radar, manteniendo el anillo exterior de grados fijo mientras se ajusta la rejilla interna.
Trazado de Camino Preciso (Breadcrumb Trail): Grabación de rutas con líneas delgadas, estilizadas y suavizadas para evitar manchas o saltos imprecisos en la interfaz.
Ficha Técnica en Español Claro: Cada waypoint guardado almacena y muestra de forma legible:
Nombre e icono personalizado.
Altitud barométrica real.
Distancia exacta (en metros o kilómetros).
Rumbo en grados y punto cardinal en español (ej. 304° NO - Noroeste).
Desnivel y coordenadas geográficas.
🛠️ Stack Tecnológico
Lenguaje: Kotlin
Interfaz de Usuario: Canvas API personalizada para renderizado 3D y gestos táctiles.
Base de Datos: Room Database para persistencia local de waypoints y trayectos.
Sensores: Sensor Framework de Android (SensorManager, acelerómetro, magnetómetro y barómetro).
📋 Licencia y Autoría
Este proyecto es de código abierto bajo los términos de la Licencia MIT, lo que significa que puedes usarlo, modificarlo y compartirlo libremente.
Creado y Desarrollado por: JAHSEH (@knzz)
Portafolio Web: codebuds.site
