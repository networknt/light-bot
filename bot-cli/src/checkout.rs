use std::fs;
use std::path::Path;
use std::path::PathBuf;
use std::env;
use std::process::{Command, Stdio};

static WORKSPACE: &'static str = "workspace";


pub fn checkout()
{

    println!("checkout!");

    let mut path: PathBuf = get_app_dir();
    path.push(WORKSPACE);

    //let str_path = path.to_str().unwrap();
    if path_exists(&path) {
        println!("workspace exists");
        path.push("light-4j");
        if path_exists(&path) {
            // checkout develop branch
            checkout_branch("develop", &path);
            maven_build(&path);
        } else {
            clone_repo("git@github.com:networknt/light-4j.git", &path);
        }

    } else {
        println!("workspace doesn't exist");
        // create it.
        create_workspace(&path);
        clone_repo("git@github.com:networknt/light-4j.git", &path);
    }


}

fn create_workspace(path: &PathBuf) {
    match fs::create_dir(path.to_str().unwrap()) {
        Err(why) => println!("! {:?}", why.kind()),
        Ok(_) => {},
    }
}

fn maven_build(path: &PathBuf) {
    let child = Command::new("mvn")
        .arg("clean")
        .arg("install")
        .current_dir(path.to_str().unwrap())
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to execute child");

    let output = child
        .wait_with_output()
        .expect("failed to wait on child");

    assert!(output.status.success());

}

fn checkout_branch(branch: &str, path: &PathBuf) {
    let child = Command::new("git")
        .arg("checkout")
        .arg(branch)
        .current_dir(path.to_str().unwrap())
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to execute child");

    let output = child
        .wait_with_output()
        .expect("failed to wait on child");

    assert!(output.status.success());


}

fn clone_repo(repo: &str, path: &PathBuf) {
    let child = Command::new("git")
        .arg("clone")
        .arg(repo)
        .current_dir(path.to_str().unwrap())
        .stdout(Stdio::piped())
        .spawn()
        .expect("failed to execute child");

    let output = child
        .wait_with_output()
        .expect("failed to wait on child");

    assert!(output.status.success());
}


fn get_app_dir() -> PathBuf {
    let dir: PathBuf = match env::home_dir() {
        Some(path) => PathBuf::from(path),
        None => PathBuf::from(""),
    };
    dir
}

pub fn path_exists(path: &PathBuf) -> bool {
    return Path::new(&path).exists();
}