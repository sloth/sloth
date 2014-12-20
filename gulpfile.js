var gulp = require("gulp"),
    concat = require("gulp-concat"),
    uglify = require("gulp-uglify"),
    plumber = require("gulp-plumber"),
    rename = require("gulp-rename"),
    jade = require("gulp-jade"),
    scss = require("gulp-ruby-sass"),
    wrap = require("gulp-wrap"),
    template = require("gulp-template"),
    del = require("del"),
    runSequence = require("run-sequence"),
    zip = require('gulp-zip'),
    pkg = require('./package.json');

var paths = {};
paths.dist = "resources/public/";
paths.app = "resources/assets/";
paths.js = [
    paths.app + "js/stanza/stanzaio.bundle.min.js",
    paths.app + "js/react/react-0.11.2.min.js"
];
paths.scss = paths.app + "scss/**/*.scss";

gulp.task("scss", function() {
    return gulp.src(paths.app + "scss/main.scss")
        .pipe(plumber())
        .pipe(scss({bundleExec: true,
                    "sourcemap=none": true}))
        .pipe(concat("main.css"))
        .pipe(gulp.dest(paths.dist + "styles/"));
});

gulp.task("styles", ["scss"]);

gulp.task("js", function() {
    return gulp.src(paths.js)
        .pipe(plumber())
        .pipe(concat("vendor.js"))
        .pipe(gulp.dest(paths.dist + "js/"));
});

gulp.task("watch", function() {
    gulp.watch(paths.scss, ["styles"]);
    gulp.watch(paths.js, ["js"]);
    gulp.watch(paths.images, ["copy-images"]);
    gulp.watch(paths.fonts, ["copy-fonts"]);
});

gulp.task("serve", function(){
    var express = require("express");

    var app = express(),
        resources = __dirname + "/resources/";

    app.use("/sandbox", express.static(resources + "sandbox/"));
    app.use("/debug/static", express.static(resources + "public/"));
    app.use("/release/static", express.static(resources + "public/"));
    app.use("/static", express.static(resources + "public/"));

    app.all("/debug/*", function(req, res, next){
        res.sendFile("index.debug.html", {root: resources});
    });

    app.all("/release/*", function(req, res, next){
        res.sendFile("index.release.html", {root: resources});
    });

    app.listen(9000);
});

gulp.task("clean:target", function(cb) {
    del("target/nw/**/*", cb);
})

gulp.task("copy:fonts", function() {
    return gulp.src("resources/assets/fonts/*")
        .pipe(gulp.dest("target/nw/static/fonts/"));
});

gulp.task("copy:images", function() {
    return gulp.src("resources/assets/imgs/**/*")
        .pipe(gulp.dest("target/nw/static/imgs/"));
});

gulp.task("copy:app", function() {
    return gulp.src("resources/public/js/app.js")
        .pipe(gulp.dest("target/nw/static/js/"));
});

gulp.task("copy:vendor", function() {
    return gulp.src("resources/public/js/vendor.js")
        .pipe(gulp.dest("target/nw/static/js/"));
});

gulp.task("copy:html", function() {
    return gulp.src("resources/index.nw.html")
        .pipe(rename("index.html"))
        .pipe(gulp.dest("target/nw/"));
});

gulp.task("copy:packagejson", function() {
    return gulp.src("package.nw.json")
        .pipe(rename("package.json"))
        .pipe(gulp.dest("target/nw/"));
});

gulp.task("copy:styles", function() {
    return gulp.src("resources/public/styles/main.css")
        .pipe(gulp.dest("target/nw/static/styles/"));
});

gulp.task("copy", ["copy:fonts", "copy:images", "copy:app",
                   "copy:vendor", "copy:html", "copy:styles", "copy:packagejson"]);

gulp.task("nw:prepare", function(cb) {
    runSequence("styles", "js", "clean:target", "copy", cb);
});

gulp.task("nw:package", function(cb) {
    return gulp.src("target/nw/**/*")
        .pipe(zip("sloth-" + pkg.version + ".nw"))
        .pipe(gulp.dest("target/"));
});

gulp.task("package", function(cb) {
    runSequence("nw:prepare", "nw:package");
})

gulp.task("default", [
    "styles",
    "js",
    "watch",
    "serve"
]);
