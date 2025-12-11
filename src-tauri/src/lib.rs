#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use tauri::Builder;

// 这里复用你原来 main.rs 里的 setup / command / plugin
#[no_mangle]
pub extern "C" fn tauri_create_app() {
  Builder::default()
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
