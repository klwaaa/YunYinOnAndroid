use serde::de::DeserializeOwned;
use tauri::{
  plugin::{PluginApi, PluginHandle},
  AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_media_notification);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
  _app: &AppHandle<R>,
  api: PluginApi<R, C>,
) -> crate::Result<MediaNotification<R>> {
  #[cfg(target_os = "android")]
  let handle = api.register_android_plugin("com.plugin.media_notification", "ExamplePlugin")?;
  #[cfg(target_os = "ios")]
  let handle = api.register_ios_plugin(init_plugin_media_notification)?;
  Ok(MediaNotification(handle))
}

/// Access to the media-notification APIs.
pub struct MediaNotification<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> MediaNotification<R> {
  pub fn start_notification(&self, notification_args:NotificationArgs) -> crate::Result<Notification> {
    self.0
      .run_mobile_plugin("startNotification",notification_args)
      .map_err(Into::into)
  }
}
