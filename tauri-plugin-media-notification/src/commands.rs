use tauri::{command, AppHandle, Runtime};

use crate::models::*;
use crate::MediaNotificationExt;
use crate::Result;

#[command]
pub(crate) async fn ping<R: Runtime>(
    app: AppHandle<R>,
    payload: PingRequest,
) -> Result<PingResponse> {
    app.media_notification().ping(payload)
}

#[command]
pub(crate) fn start_notification<R: Runtime>(app: AppHandle<R>) -> Result<Notification> {
    app.media_notification().start_notification()
}
