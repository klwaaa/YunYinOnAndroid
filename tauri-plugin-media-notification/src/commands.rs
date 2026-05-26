use tauri::{command, AppHandle, Runtime};

use crate::models::*;
use crate::MediaNotificationExt;
use crate::Result;

#[command]
pub(crate) fn start_notification<R: Runtime>(
    app: AppHandle<R>,
    notification_args: NotificationArgs,
) -> Result<Notification> {
    app.media_notification()
        .start_notification(notification_args)
}
