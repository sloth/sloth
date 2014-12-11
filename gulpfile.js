var gulp = require("gulp")

var concat = require("gulp-concat");
var uglify = require("gulp-uglify");
var plumber = require("gulp-plumber");
var rename = require("gulp-rename");
var jade = require("gulp-jade");
var scss = require("gulp-ruby-sass")

var wrap = require("gulp-wrap");
var uglify = require("gulp-uglify");
var template = require("gulp-template");
var del = require("del");
var runSequence = require("run-sequence");

var paths = {};
paths.dist = "resources/public/";
paths.app = "resources/"
paths.js = [
    "resources/js/stanza/stanzaio.bundle.min.js"
]

gulp.task("scss", function() {
    return gulp.src(paths.app + "styles/app/main.scss")
        .pipe(plumber())
        .pipe(scss({bundleExec: true,
                    "sourcemap=none": true}))
        .pipe(concat("main.css"))
        .pipe(gulp.dest(paths.dist + "styles/"));
});

gulp.task("styles", ["scss"])

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

gulp.task("default", [
    "styles",
    "js",
    "watch"
]);
