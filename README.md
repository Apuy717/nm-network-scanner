## Native Turbo Module Scanner tasomota sonoff

#### desctiption

Native Turbo Module is a React Native library used to scan IP addresses on the same network, for IoT devices using Tasmota (Sonoff) software.

#### device supported

- android only
- testing using android 15

#### how to install

##### 1. Copy folder and file specs to root project

```bash
cp -r specs ./your root project
```

##### 2. Copy folder networkscanner to directorey

```
RootProject/
├── android/
│ ├── app/
│ │ ├── src/
│ │ │ ├── main/
│ │ │ │ ├── java/
│ │ │ │ │ ├── networkscanner/
```

</br>

##### 3. Add code

```sh
RootProject/android/app/src/main/java/com/yourapp/MainApplication.kt
+import networkscanner.NetworkScannerPackage
+add(NetworkScannerPackage())
```

```kotlin
//sample
package com.yourapp

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.react.soloader.OpenSourceMergedSoMapping
import com.facebook.soloader.SoLoader
import networkscanner.NetworkScannerPackage // New

class MainApplication : Application(), ReactApplication {

  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              // Packages that cannot be autolinked yet can be added manually here, for example:
              // add(MyReactNativePackage())
                add(NetworkScannerPackage()) // new
            }

        override fun getJSMainModuleName(): String = "index"

        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }

  override val reactHost: ReactHost
    get() = getDefaultReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, OpenSourceMergedSoMapping)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      // If you opted-in for the New Architecture, we load the native entry point for this app.
      load()
    }
  }
}
```

</br>

##### 4. Add to your package.json

```json
package.json

"codegenConfig": {
  "name": "NativeNetworkScannerSpec",
  "type": "modules",
  "jsSrcsDir": "specs",
  "android": {
    "javaPackageName": "com.networkscanner"
  }
}
```

</br>

##### 5. Generate spec using gradle command

```sh
cd ./android
./gradlew clean # optional
./gradlew generateCodegenArtifactsFromSchema

#result
BUILD SUCCESSFUL in 837ms
14 actionable tasks: 3 executed, 11 up-to-date
```

##### 6. Sample using in react-native js/ts

```tsx
App.tsx;

import { useState } from "react";
import { Button, Text, View } from "react-native";
import NativeNetworkScanner from "./specs/NativeNetworkScanner";

export default function App() {
  const [devices, setDevices] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

  async function scanNetwork() {
    setLoading(true);
    try {
      const result = await NativeNetworkScanner.scanNetwork();
      console.log("Devices found:", result);
      setDevices(result);
      setLoading(false);
    } catch (error) {
      console.error("Scan error:", error);
      setLoading(false);
    }
  }

  return (
    <View style={{ paddingTop: 50 }}>
      <Button title={`${loading ? "Loading..." : "Scan Network"}`} onPress={scanNetwork} />
      {devices.map((device, index) => (
        <Text key={index}>{device}</Text>
      ))}
    </View>
  );
}
```
