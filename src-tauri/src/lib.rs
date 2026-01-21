#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use indexmap::IndexMap;
use reqwest::Client;
use serde::{Deserialize, Serialize};
use serde_json::json;
use std::fs::File;
use std::io::{Read, Write};
use std::path::PathBuf;
use std::thread;
use std::time::Duration;
use tauri::{command, AppHandle, Manager};

use material_colors::color::Argb;
use material_colors::theme::{Theme, ThemeBuilder};

/// 阿里云 token 接口地址
const TOKEN_URL: &str = "https://openapi.alipan.com/oauth/access_token";

/// 音频项结构
#[derive(Debug, Serialize, Deserialize)]
pub struct AudioFile {
    name: String,
    #[serde(rename = "fileId")]
    file_id: String,
    duration: String,
}

/// 每个歌单项是一个 map：如 { "a": [AudioFile, ...] }
pub type PlaylistItem = IndexMap<String, Vec<AudioFile>>;

/// 整体播放列表是多个歌单项组成的数组
pub type PlayListData = Vec<PlaylistItem>;

/// 获取 app_data_dir 路径 + 确保目录存在
fn ensure_app_file(app: &AppHandle, filename: &str) -> Result<PathBuf, String> {
    let dir = app
        .path()
        .app_data_dir()
        .map_err(|e| format!("无法获取 app_data_dir: {}", e))?;

    // 确保目录存在
    std::fs::create_dir_all(&dir).map_err(|e| format!("无法创建目录: {}", e))?;

    Ok(dir.join(filename))
}

/// 使用授权码换取 token
#[command]
async fn get_token_by_code(code: String) -> Result<String, String> {
    let client_id = "应用id";
    let client_secret = "应用密钥";

    let client = Client::new();
    let res = client
        .post(TOKEN_URL)
        .json(&json!({
            "client_id": client_id,
            "client_secret": client_secret,
            "grant_type": "authorization_code",
            "code": code,
        }))
        .send()
        .await
        .map_err(|e| format!("请求失败：{}", e))?;

    let status = res.status();
    let text = res.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("请求失败 {}: {}", status, text))
    }
}

/// 使用 refresh_token 刷新 token
#[command]
async fn get_token_by_refresh(refresh_token: String) -> Result<String, String> {
    let client_id = "应用id";
    let client_secret = "应用密钥";

    let client = Client::new();
    let res = client
        .post(TOKEN_URL)
        .json(&json!({
            "client_id": client_id,
            "client_secret": client_secret,
            "grant_type": "refresh_token",
            "refresh_token": refresh_token,
        }))
        .send()
        .await
        .map_err(|e| format!("请求失败：{}", e))?;

    let status = res.status();
    let text = res.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("刷新失败 {}: {}", status, text))
    }
}

/// getDriveId
#[command]
async fn get_drive_id(token: String) -> Result<String, String> {
    let client = Client::new();
    let url = "https://openapi.alipan.com/adrive/v1.0/user/getDriveInfo";

    let response = client
        .post(url)
        .header("Authorization", format!("Bearer {}", token))
        .send()
        .await
        .map_err(|e| e.to_string())?;

    let status = response.status();
    let text = response.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("获取失败 {}: {}", status, text))
    }
}

/// 读取 data.json
#[command]
fn get_all_audio_data(app: AppHandle) -> Result<PlayListData, String> {
    let path = ensure_app_file(&app, "data.json")?;

    let content = std::fs::read_to_string(&path).unwrap_or("[]".to_string()); // 默认空数组

    let parsed: PlayListData =
        serde_json::from_str(&content).map_err(|e| format!("JSON 解析失败: {}", e))?;

    Ok(parsed)
}

/// 写入 data.json
#[command]
fn update_playlist_data(app: AppHandle, data: PlayListData) -> Result<(), String> {
    let path = ensure_app_file(&app, "data.json")?;

    let json = serde_json::to_string_pretty(&data).map_err(|e| e.to_string())?;

    std::fs::write(&path, json).map_err(|e| format!("写入失败: {}", e))?;

    Ok(())
}

/// 上传 data.json
#[command]
async fn upload_data_json(app: AppHandle, upload_url: String) -> Result<String, String> {
    let path = ensure_app_file(&app, "data.json")?;

    let mut file = File::open(&path).map_err(|e| format!("打开失败: {}", e))?;

    let mut buffer = Vec::new();
    file.read_to_end(&mut buffer)
        .map_err(|e| format!("读取失败: {}", e))?;

    let client = reqwest::Client::new();
    let res = client
        .put(&upload_url)
        .body(buffer)
        .send()
        .await
        .map_err(|e| format!("上传失败: {}", e))?;

    let status = res.status();
    let text = res.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(format!("上传成功 {}", text))
    } else {
        Err(format!("上传失败 {}: {}", status, text))
    }
}

/// get_by_path
#[command]
async fn using_path_get_data(
    drive_id: String,
    token: String,
    file_path: String,
) -> Result<String, String> {
    let client = Client::new();
    let url = "https://openapi.alipan.com/adrive/v1.0/openFile/get_by_path";

    let res = client
        .post(url)
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "drive_id": drive_id,
            "file_path": file_path
        }))
        .send()
        .await
        .map_err(|e| format!("请求失败: {}", e))?;

    let status = res.status();
    let text = res.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("失败 {}: {}", status, text))
    }
}

/// 列出文件
#[command]
async fn get_file_list(
    drive_id: String,
    parent_file_id: String,
    next_marker: String,
    token: String,
) -> Result<String, String> {
    let client = Client::new();

    let res = client
        .post("https://openapi.alipan.com/adrive/v1.0/openFile/list")
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "drive_id": drive_id,
            "parent_file_id": parent_file_id,
            "limit": 100,
            "category": "audio",
            "type": "file",
            "marker": next_marker
        }))
        .send()
        .await
        .map_err(|e| format!("请求失败: {}", e))?;

    let status = res.status();
    let text = res.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("失败 {}: {}", status, text))
    }
}

/// 获取音频下载链接
#[command]
async fn get_audio_url(drive_id: String, file_id: String, token: String) -> Result<String, String> {
    let client = Client::new();

    let resp = client
        .post("https://openapi.alipan.com/adrive/v1.0/openFile/getDownloadUrl")
        .header("Authorization", token)
        .json(&json!({
            "drive_id": drive_id,
            "file_id": file_id
        }))
        .send()
        .await
        .map_err(|e| format!("请求失败: {}", e))?
        .text()
        .await
        .map_err(|e| format!("读取失败: {}", e))?;

    Ok(resp)
}

/// 获取资料文件地址
#[command]
async fn get_data_url(drive_id: String, file_id: String, token: String) -> Result<String, String> {
    let client = Client::new();

    let response = client
        .post("https://openapi.alipan.com/adrive/v1.0/openFile/get")
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "drive_id": drive_id,
            "file_id": file_id
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;

    let status = response.status();
    let text = response.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("失败 {}: {}", status, text))
    }
}

/// 放入回收站
#[command]
async fn put_in_recycle_bin(
    token: String,
    drive_id: String,
    file_id: String,
) -> Result<String, String> {
    let client = reqwest::Client::new();

    let response = client
        .post("https://openapi.alipan.com/adrive/v1.0/openFile/recyclebin/trash")
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "drive_id": drive_id,
            "file_id": file_id
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;

    let status = response.status();
    let text = response.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("失败 {}: {}", status, text))
    }
}

/// 创建文件
#[command]
async fn create_file(
    drive_id: String,
    parent_file_id: String,
    token: String,
) -> Result<String, String> {
    let client = Client::new();

    let response = client
        .post("https://openapi.alipan.com/adrive/v1.0/openFile/create")
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "drive_id": drive_id,
            "parent_file_id": parent_file_id,
            "name": "data.json",
            "type": "file",
            "check_name_mode": "refuse"
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;

    let text = response.text().await.unwrap_or_default();
    Ok(text)
}

/// 标记上传完毕
#[command]
async fn complete_upload(
    drive_id: String,
    file_id: String,
    upload_id: String,
    token: String,
) -> Result<String, String> {
    let client = Client::new();

    let response = client
        .post("https://openapi.alipan.com/adrive/v1.0/openFile/complete")
        .header("Authorization", format!("Bearer {}", token))
        .json(&json!({
            "drive_id": drive_id,
            "file_id": file_id,
            "upload_id": upload_id
        }))
        .send()
        .await
        .map_err(|e| e.to_string())?;

    let status = response.status();
    let text = response.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(text)
    } else {
        Err(format!("失败 {}: {}", status, text))
    }
}

#[derive(Deserialize, Debug)]
struct SourceColor {
    source: u32,
}

/// 莫奈取色
#[command]
fn material_colors(source: u32) -> Theme {
    ThemeBuilder::with_source(Argb::from_u32(source)).build()
}

/// 从本地 themeColor.json 读取主题
#[command]
fn get_theme_color_from_local(app: AppHandle) -> Result<Theme, String> {
    let path = ensure_app_file(&app, "themeColor.json")?;

    let content =
        std::fs::read_to_string(&path).unwrap_or_else(|_| r#"{"source":4294962455}"#.to_string());

    let color: SourceColor = serde_json::from_str(&content).map_err(|e| e.to_string())?;

    Ok(ThemeBuilder::with_source(Argb::from_u32(color.source)).build())
}

/// 更新 themeColor.json
#[command]
fn update_theme_color(app: AppHandle, color_source: String) -> Result<(), String> {
    let path = ensure_app_file(&app, "themeColor.json")?;

    std::fs::write(&path, color_source).map_err(|e| format!("写入失败: {}", e))?;

    Ok(())
}

/// Tauri 入口
#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![
            get_token_by_code,
            get_token_by_refresh,
            get_drive_id,
            get_all_audio_data,
            update_playlist_data,
            upload_data_json,
            using_path_get_data,
            get_file_list,
            get_audio_url,
            get_data_url,
            put_in_recycle_bin,
            create_file,
            complete_upload,
            material_colors,
            get_theme_color_from_local,
            update_theme_color
        ])
        .run(tauri::generate_context!())
        .expect("运行 Tauri 应用失败");
}
