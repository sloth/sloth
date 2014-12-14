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
    runSequence = require("run-sequence");

var paths = {};
paths.dist = "resources/public/";
paths.app = "resources/assets/";
paths.js = [
    paths.app + "js/stanza/stanzaio.bundle.min.js"
];

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

gulp.task("copy-fonts", function() {
    return gulp.src(paths.fonts)
        .pipe(gulp.dest(paths.dist + "fonts/"));
});

gulp.task("copy-images", function() {
    return gulp.src(paths.images)
        .pipe(gulp.dest(paths.dist + "images/"));
});

gulp.task("copy", ["copy-fonts", "copy-images"]);

gulp.task("watch", function() {
    gulp.watch(paths.scss, ["styles"]);
    gulp.watch(paths.js, ["js"]);
    gulp.watch(paths.images, ["copy-images"]);
    gulp.watch(paths.fonts, ["copy-fonts"]);
});

gulp.task("serve", function(){
    var express = require("express");

    var app = express(),
        resources = __dirname + "/" + paths.dist;

    app.use("/static", express.static(resources));
    app.all("/*", function(req, res, next){
        res.sendFile("index.html", {root: resources});
    });

    app.listen(9000);
});

gulp.task("default", [
    "styles",
    "js",
    "watch",
    "serve"
]);
