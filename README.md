# Сто лет как устарело.


# Быстрая интеграция Google Chromecast в Android приложение
## Введение
Добрый день, сегодня я хочу рассказать вам о том, как очень быстро добавить
в приложение возможность взаимодействовать с устройством Google Chromecast, а
именно - отправлять один видеофайл на воспроизведение и управлять просмотром.
Если вы не знакомы с устройством Chromecast, то можете почитать обзорную статью
[вот тут](https://geektimes.ru/post/187870/). Несмотря на то, что эта статья
про первую версию Chromecast, она даст общее представление о всем семействе
устройтсв и принципе их работы.

## Первые шаги
Приступим к интеграции Chromecast в наше Android приложение. Мы рассмотрим
простейший случай, когда в приложении имеется Activity, содержащая некоторый
видео контент(один видео файл). Для этого воспользуемся библиотекой
[CastCompanionLibrary-android](https://github.com/googlecast/CastCompanionLibrary-android),
которая упрощает интеграцию до нескольких шагов.

Для начала создадим пустой проект в Android Studio и добавим в файл app/build.gradle
зависимость.
```groovy
dependencies {
    compile 'com.google.android.libraries.cast.companionlibrary:ccl:2.8.4'
}
```
Библиотека использует синглтон VideoCastManager для организации взаимодействия.
В первую очередь, мы должны инициализировать этот синглтон при помощи объекта
конфигурации. Большинство опций прокомментировано в коде.
```java
// Core.java
public class Core extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CastConfiguration options = new CastConfiguration.Builder("CC1AD845")
                .enableAutoReconnect() // Восстановление соединения после разрыва
                .enableDebug() // Разрешаем отладку, чтобы логи были подробными
                .enableLockScreen() // Возможность управления на экране блокировки
                .enableNotification() // Возможность управления через оповещение + возможные действия
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_REWIND, false)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_PLAY_PAUSE, true)
                .addNotificationAction(CastConfiguration.NOTIFICATION_ACTION_DISCONNECT, true)
                .enableWifiReconnection() // Восстановление, после смены wifi сети
                .setForwardStep(10) // Шаг перемотки в секундах
                .build();
        VideoCastManager.initialize(this, options);
    }
}
```
В конструктор CastConfiguration мы передаем идентификатор Media Receiver.
Этот идентификатор определяет стилизацию плеера Chromecast. Мы не будем
останавливаться на нем, более подробно можно почитать [на официальной странице](https://developers.google.com/cast/docs/registration).
Информацию о других опциях VideoCastManager можно найти [в github](https://github.com/googlecast/CastCompanionLibrary-android/blob/master/src/com/google/android/libraries/cast/companionlibrary/cast/CastConfiguration.java).

## Воспроизведение одного файла
Для организации взаимодействия между Chromecast и приложением Android библиотека
использует класс VideoCastConsumerImpl. Изначально он рассчитан для работы с
очередью видеофайлов, но ,т.к. наше приложение не предполагает наличие очереди,
мы несколько изменим этот класс.
```java
// SingleVideoCastConsumer.java
public abstract class SingleVideoCastConsumer extends VideoCastConsumerImpl {
    private AppCompatActivity activity;
    private final String videoUrl;
    private final String title;
    private final String subtitle;
    private final String imageUrl;
    private final String contentType;

    public SingleVideoCastConsumer(AppCompatActivity activity, String videoUrl, String title, String subtitle, String imageUrl, String contentType) {
        this.activity = activity;
        this.videoUrl = videoUrl;
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.contentType = contentType;
    }

    public abstract void onPlaybackFinished();
    public abstract void onQueueLoad(final MediaQueueItem[] items, final int startIndex, final int repeatMode,
                                     final JSONObject customData) throws TransientNetworkDisconnectionException, NoConnectionException;

    @Override
    public void onMediaQueueUpdated(List<MediaQueueItem> queueItems, MediaQueueItem item, int repeatMode, boolean shuffle) {
        // Если в очереди больше нет элементов, то оповещаем о завершении воспроизведения
        if(queueItems != null && queueItems.size() == 0) {
            onPlaybackFinished();
        }
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                       boolean wasLaunched) {
        // Изменить состояние кнопки Cast
        activity.invalidateOptionsMenu();
        // Создаем метаданные типа видеофайл
        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);

        // Заголовок
        movieMetadata.putString(MediaMetadata.KEY_TITLE, title);

        // Подзаголовок
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, subtitle);

        // Картинка, которая будет показана при загрузке
        movieMetadata.addImage(new WebImage(Uri.parse(imageUrl)));

        // Создаем информацию о медиа контенте
        MediaInfo info = new MediaInfo.Builder(videoUrl)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType(contentType)
                .setMetadata(movieMetadata)
                .build();

        // Создаем элемент очереди медиафайлов
        MediaQueueItem item = new MediaQueueItem.Builder(info).build();
        try {
            // Обновляем очередь Chromecast, она всегда содержит 1 элемент, т.к. у нас всего 1 видеофайл
            onQueueLoad(new MediaQueueItem[]{item}, 0, MediaStatus.REPEAT_MODE_REPEAT_OFF, null);
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected() {
        // Изменить состояние кнопки Cast
        activity.invalidateOptionsMenu();
    }
}
```

Основными методами, на которых стоит заострить внимание являются
onApplicationConnected и onQueueLoad. Как в могли заметить, библиотека использует
MediaInfo, MediaMetadata и MediaQueueItem для работы с медиа данными. в методе
onApplicationConnected, который будет вызван как только приложение подключится
к Chromecast, мы создадим объект очереди и вызовем абстрактный метод onQueueLoad,
который позже реализуем в Activity. Описание работы методов можно найти в комментариях
к коду.

## Использование в Activity
Следующим (и последним) шагом будет реализация нашей Activity.
```java
ublic class MainActivity extends AppCompatActivity {

    private VideoCastManager castManager;
    private VideoCastConsumer castConsumer;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        castManager = VideoCastManager.getInstance();
        castConsumer = new SingleVideoCastConsumer(this,
                http://example.com/somemkvfile.mkv", // ссылка на файл
                "Jet Packs Was Yes", "Periphery", // подзаголовок и заголовок
                "http://fugostudios.com/wp-content/uploads/2012/02/periphery720p-600x338.jpg", // картинка
                "video/mkv" // тип файла
                ) {
            @Override
            public void onPlaybackFinished() {
                // Отключаем устройство
                disconnectDevice();
            }

            @Override
            public void onQueueLoad(MediaQueueItem[] items, int startIndex,
                                    int repeatMode, JSONObject customData)
                    throws TransientNetworkDisconnectionException, NoConnectionException {
                // Простой проброс очереди из нашего SingleVideoCastConsumer в castManager
                castManager.queueLoad(items, startIndex, repeatMode, customData);
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        // Добавляем кнопку Cast в toolbar
        castManager.addMediaRouterButton(menu, R.id.media_route_menu_item);
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        // Даем возможность управлять громкостью воспроизведения при помощи
        // физических кнопок
        return castManager.onDispatchVolumeKeyEvent(event, 0.05)
                || super.dispatchKeyEvent(event);
    }

    @Override
    protected void onResume() {
        // Подключаем castConsumer и увеличиваем счетчик подключений
        if (castManager != null) {
            castManager.addVideoCastConsumer(castConsumer);
            castManager.incrementUiCounter();
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        // Уменьшаем счетчик подключений и отключаем castConsumer
        castManager.decrementUiCounter();
        castManager.removeVideoCastConsumer(castConsumer);
        super.onPause();
    }

    // По непонятной мне причине отключение устройтства без задержки
    // не работало, но если использовать 100-500 мс задержку, то устройство
    // отключается нормально.
    private void disconnectDevice() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               castManager.disconnect();
            }
        },500);
    }
}
```

В нашей Activity нет ничего сложного, мы получаем VideoCastManager в методе
onCreate. В методах onResume и onPause управляем жизненным циклом нашего
подключения к Chromecast. А методы onCreateOptionsMenu и dispatchKeyEvent
организуют UX часть нашей интеграции.
К сожалению, я так и не понял, почему castManager.disconnect() выбрасывал ошибку,
но какая программа обходится без костылей :)

## Design Checklist
Теперь обратимся к дизайну. Большинство из Design Guidelines за нас реализует вышеописанная библиотка, но некоторые пункты нужно реализовать вручную.
* Стилизация диалогов
* Показ интро для пользователя
* Стилизация ресивера
Мы выполняли интеграцию с Google Chromecast в приложении "Рецепты Юлии Высоцкой". В этом приложении присутствуют видео-рецепты и было бы неплохо добавить возможность показывать их через Chromecast.

Если к рецепту прикреплен видео-файл, то мы даем возможность пользователю просмотреть его через приложение на его выбор. Это выглядит вот так.

КАРТИНКА

После интеграции с Chromecast и при наличии в нашей сети настроенного Chromecast экран будет выглядеть так.

КАРТИНКА

Если пользователь в первый раз подключил Chromecast, то мы должны оповестить его, что в нашем приложении присутствует возможность показывать видео рецепты через него.

```java
.setMediaRouteDialogFactory(new MediaRouteDialogFactory() {

                    @NonNull
                    @Override
                    public MediaRouteChooserDialogFragment onCreateChooserDialogFragment() {
                        return new MediaRouteChooserDialogFragment() {
                            @Override
                            public MediaRouteChooserDialog onCreateChooserDialog(Context context, Bundle savedInstanceState) {
                                return new MediaRouteChooserDialog(context, R.style.Theme_MediaRouter_Light);
                            }
                        };
                    }

                    @NonNull
                    @Override
                    public MediaRouteControllerDialogFragment onCreateControllerDialogFragment() {
                        return new MediaRouteControllerDialogFragment(){
                            @Override
                            public MediaRouteControllerDialog onCreateControllerDialog(Context context, Bundle savedInstanceState) {
                                return new MediaRouteControllerDialog(context, R.style.Theme_MediaRouter_Light);
                            }
                        };
                    }
                })
```

```xml
<style name="Theme.MediaRouter.Light" parent="Base.Theme.AppCompat.Light.Dialog.Alert">
        <item name="android:windowNoTitle">false</item>
        <item name="mediaRouteButtonStyle">@style/Widget.MediaRouter.Light.MediaRouteButton</item>
        <item name="MediaRouteControllerWindowBackground">@drawable/mr_dialog_material_background_light</item>
        <item name="mediaRouteOffDrawable">@drawable/ic_cast_off_light</item>
        <item name="mediaRouteConnectingDrawable">@drawable/mr_ic_media_route_connecting_mono_light</item>
        <item name="mediaRouteOnDrawable">@drawable/ic_cast_on_light</item>
        <item name="mediaRouteCloseDrawable">@drawable/mr_ic_close_light</item>
        <item name="mediaRoutePlayDrawable">@drawable/mr_ic_play_light</item>
        <item name="mediaRoutePauseDrawable">@drawable/mr_ic_pause_light</item>
        <item name="mediaRouteCastDrawable">@drawable/mr_ic_cast_light</item>
        <item name="mediaRouteAudioTrackDrawable">@drawable/mr_ic_audiotrack_light</item>
        <item name="mediaRouteDefaultIconDrawable">@drawable/ic_cast_grey</item>
        <item name="mediaRouteBluetoothIconDrawable">@drawable/ic_bluetooth_grey</item>
        <item name="mediaRouteTvIconDrawable">@drawable/ic_tv_light</item>
        <item name="mediaRouteSpeakerIconDrawable">@drawable/ic_speaker_light</item>
        <item name="mediaRouteSpeakerGroupIconDrawable">@drawable/ic_speaker_group_light</item>
        <item name="mediaRouteChooserPrimaryTextStyle">@style/Widget.MediaRouter.ChooserText.Primary.Light</item>
        <item name="mediaRouteChooserSecondaryTextStyle">@style/Widget.MediaRouter.ChooserText.Secondary.Light</item>
        <item name="mediaRouteControllerTitleTextStyle">@style/Widget.MediaRouter.ControllerText.Title.Dark</item>
        <item name="mediaRouteControllerPrimaryTextStyle">@style/Widget.MediaRouter.ControllerText.Primary.Light</item>
        <item name="mediaRouteControllerSecondaryTextStyle">@style/Widget.MediaRouter.ControllerText.Secondary.Light</item>
    </style>
```

```xml
<receiver android:name="com.google.android.libraries.cast.companionlibrary.remotecontrol.VideoIntentReceiver" >
    <intent-filter>
        <action android:name="android.media.AUDIO_BECOMING_NOISY" />
        <action android:name="android.intent.action.MEDIA_BUTTON" />
        <action android:name="com.google.android.libraries.cast.companionlibrary.action.toggleplayback" />
        <action android:name="com.google.android.libraries.cast.companionlibrary.action.stop" />
    </intent-filter>
</receiver>

<service
    android:name="com.google.android.libraries.cast.companionlibrary.notification.VideoCastNotificationService"
    android:exported="false" >
    <intent-filter>
        <action android:name="com.google.android.libraries.cast.companionlibrary.action.toggleplayback" />
        <action android:name="com.google.android.libraries.cast.companionlibrary.action.stop" />
        <action android:name="com.google.android.libraries.cast.companionlibrary.action.notificationvisibility" />
    </intent-filter>
</service>

<service android:name="com.google.android.libraries.cast.companionlibrary.cast.reconnection.ReconnectionService"/>
<activity android:name="com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity"/>
```
Надеюсь, что вы нашли статью полезной, полный исходных код проекта
лежит [на github](https://github.com/arkty/caster). Задавайте вопросы в
комментариях, в следующей статье я постараюсь собрать ответы на часто
задаваемые вопросы и рассказать о Media Receivers, управлении очередью
воспроизведения, а также о стилизации диалогов, которые использует
VideoCastManager для взаимодействия с пользователями.

Более полную информацию об интеграции с другими платформами, а также примеры
вы можете найти на [официальной странице](https://developers.google.com/cast/).
Более подробные примеры кода, в том числе и для других платформ, можно найти
[здесь](https://developers.google.com/cast/docs/downloads).
Подробную информацию о принципах взавимодействия с пользователем можно найти в
[Design Checklist](https://developers.google.com/cast/docs/design_checklist/).
