# Stage 1: базовый образ с инструментами
FROM ubuntu:22.04 AS base

# Установим необходимые инструменты (без Android SDK)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    wget \
    unzip \
    openjdk-17-jdk-headless \
    git \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=${PATH}:${JAVA_HOME}/bin

# Stage 2: установка Android SDK (отдельный слой для кэширования)
FROM base AS android-sdk

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# Создаём директории
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools

# Скачиваем и устанавливаем Command Line Tools
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/tools.zip && \
    unzip /tmp/tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/tools.zip

# Устанавливаем SDK платформы и build-tools
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses && \
    sdkmanager --sdk_root=${ANDROID_HOME} \
    "platforms;android-34" \
    "build-tools;34.0.0" \
    "platform-tools" \
    "emulator"

# Stage 3: финальный образ с приложением
FROM android-sdk AS builder

WORKDIR /app

# Копируем только gradle-related файлы для кэширования зависимостей
COPY mobile/gradle ./mobile/gradle
COPY mobile/gradlew ./mobile/
COPY mobile/build.gradle.kts ./mobile/
COPY mobile/settings.gradle.kts ./mobile/
COPY mobile/gradle.properties ./mobile/

RUN chmod +x mobile/gradlew

# Кэшируем gradle зависимости (этот слой переиспользуется если gradle-файлы не изменились)
RUN cd mobile && ./gradlew --version

# Копируем весь исходный код
COPY mobile ./mobile

RUN chmod +x mobile/gradlew

# Переменные окружения
ENV GRADLE_USER_HOME=/app/.gradle
ENV GRADLE_OPTS="-Xmx4096m -Dorg.gradle.parallel=true -Dorg.gradle.workers.max=4"

# Сборка APK и тесты
RUN cd mobile && ./gradlew assembleDebug test --info

# Результаты находятся в mobile/app/build/outputs/apk/debug/app-debug.apk
RUN mkdir -p /output && \
    ls -la /app/mobile/app/build/outputs/apk/debug/ && \
    cp /app/mobile/app/build/outputs/apk/debug/app-debug.apk /output/ && \
    ls -la /output/
