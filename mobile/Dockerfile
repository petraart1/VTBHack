# Используем официальный образ Ubuntu
FROM ubuntu:22.04

# Установим необходимые инструменты
RUN apt-get update && \
    apt-get install -y wget unzip openjdk-17-jdk git curl build-essential \
    && rm -rf /var/lib/apt/lists/*

# Установим Gradle вручную (если не используешь gradle-wrapper)
ENV GRADLE_VERSION=8.5
RUN wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
    && unzip gradle-${GRADLE_VERSION}-bin.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION}/bin/gradle /usr/bin/gradle

# Установим Android SDK
ENV ANDROID_HOME /opt/android-sdk
ENV PATH ${PATH}:${ANDROID_HOME}/cmdline-tools/tools/bin:${ANDROID_HOME}/platform-tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip \
    && unzip tools.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/tools \
    && rm tools.zip

# Скачиваем SDK платформы и build-tools
RUN yes | sdkmanager --sdk_root=${ANDROID_HOME} --licenses
RUN sdkmanager --sdk_root=${ANDROID_HOME} "platforms;android-34" "build-tools;34.0.0" "platform-tools"

# Копируем проект внутрь контейнера
WORKDIR /app
COPY . .

# Устанавливаем переменную окружения для gradle cache
ENV GRADLE_USER_HOME=/cache/.gradle

# Открываем порты ADB (по необходимости)
EXPOSE 5037

# Сборка apk и Unit тесты
CMD ./gradlew assembleDebug test
