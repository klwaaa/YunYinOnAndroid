use tauri::{
  plugin::{Builder, TauriPlugin},
  Manager, Runtime,
};

pub use models::*;

#[cfg(desktop)]
mod desktop;
#[cfg(mobile)]
mod mobile;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(desktop)]
use desktop::MediaNotification;
#[cfg(mobile)]
use mobile::MediaNotification;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the media-notification APIs.
pub trait MediaNotificationExt<R: Runtime> {
  fn media_notification(&self) -> &MediaNotification<R>;
}

impl<R: Runtime, T: Manager<R>> crate::MediaNotificationExt<R> for T {
  fn media_notification(&self) -> &MediaNotification<R> {
    self.state::<MediaNotification<R>>().inner()
  }
}

/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
  Builder::new("media-notification")
    .invoke_handler(tauri::generate_handler![commands::ping,commands::start_notification])
    .setup(|app, api| {
      #[cfg(mobile)]
      let media_notification = mobile::init(app, api)?;
      #[cfg(desktop)]
      let media_notification = desktop::init(app, api)?;
      app.manage(media_notification);
      Ok(())
    })
    .build()
}
