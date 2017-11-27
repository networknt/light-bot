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
        check_build_repo("git@github.com:networknt/light-4j.git", &path);

        path.pop();
        path.push("openapi-parser");
        check_build_repo("git@github.com:networknt/openapi-parser.git", &path);

        path.pop();
        path.push("light-rest-4j");
        check_build_repo("git@github.com:networknt/light-rest-4j.git", &path);

        path.pop();
        path.push("light-graphql-4j");
        check_build_repo("git@github.com:networknt/light-graphql-4j.git", &path);

        path.pop();
        path.push("light-hybrid-4j");
        check_build_repo("git@github.com:networknt/light-hybrid-4j.git", &path);

        path.pop();
        path.push("light-codegen");
        check_build_repo("git@github.com:networknt/light-codegen.git", &path);

        path.pop();
        path.push("light-eventuate-4j");
        check_build_repo("git@github.com:networknt/light-eventuate-4j.git", &path);

        path.pop();
        path.push("light-saga-4j");
        check_build_repo("git@github.com:networknt/light-saga-4j.git", &path);

        path.pop();
        path.push("light-session-4j");
        check_build_repo("git@github.com:networknt/light-session-4j.git", &path);

        path.pop();
        path.push("light-proxy");
        check_build_repo("git@github.com:networknt/light-proxy.git", &path);

        path.pop();
        path.push("light-oauth2");
        check_build_repo("git@github.com:networknt/light-oauth2.git", &path);

        path.pop();
        path.push("light-portal");
        check_build_repo("git@github.com:networknt/light-portal.git", &path);

    } else {
        println!("workspace doesn't exist");
        // create it.
        create_workspace(&path);
        clone_repo("git@github.com:networknt/light-4j.git", &path);
        clone_repo("git@github.com:networknt/openapi-parser.git", &path);
        clone_repo("git@github.com:networknt/light-rest-4j.git", &path);
        clone_repo("git@github.com:networknt/light-graphql-4j.git", &path);
        clone_repo("git@github.com:networknt/light-hybrid-4j.git", &path);
        clone_repo("git@github.com:networknt/light-codegen.git", &path);
        clone_repo("git@github.com:networknt/light-eventuate-4j.git", &path);
        clone_repo("git@github.com:networknt/light-saga-4j.git", &path);
        clone_repo("git@github.com:networknt/light-session-4j.git", &path);
        clone_repo("git@github.com:networknt/light-proxy.git", &path);
        clone_repo("git@github.com:networknt/light-oauth2.git", &path);
        clone_repo("git@github.com:networknt/light-portal.git", &path);

    }


}

fn check_build_repo(repo: &str, path: &PathBuf) {
    println!("path = {:?}", path);
    if !path_exists(&path) {
        let mut parent_path = PathBuf::from(path);
        parent_path.pop();
        println!("parent path = {:?}", parent_path);
        clone_repo(repo, &parent_path);
    }
    checkout_branch("develop", &path);
    maven_build(&path);
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