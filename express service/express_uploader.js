const express = require('express')
const multer =  require('multer') //https://github.com/expressjs/multer
const fs =      require('fs-extra')

const extract = require('extract-zip')
const resolve = require('path').resolve // to sort out extract-zip's life

// var upload = multer({ dest: 'uploads/' })

const app = express()
const router = express.Router()
const port = 5309
//const APP_KEY = "wgY2ho5rHkFKURuITsDyGmLsIepqd8" [TODO] for production only
const APP_KEY = "test_key_only"


const verifyHeaders = (headers => {
    if(headers && headers["key"]) {
        if(headers["key"] === APP_KEY) {
            console.log("Verified")
            return true;
        }
    }

    console.log("Invalid/missing key")
    return false;
});


 const storage = multer.diskStorage({
 	destination: function (req, file, cb) {
 		cb(null, '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test')
 	},
 	filename: function (req, file, cb) {
 		cb(null, file.originalname)
 	}
 });

 const upload = multer({
 	storage: storage
 });

// Take "filepath" field containing path and make that directory if it doesn't exist, then add file to it
let uploadFiles = multer({
		storage: multer.diskStorage({
			destination: (req, file, callback) => {
				console.log(req.files, req.body);
                let path = req.body["filepath"];
                console.log('UPLOAD PATH?',path);
                if(path && typeof path !== 'undefined'){ // "truthy": if non-null/undefined/NaN/empty/0/false, plus undeclared type check
                    // Ensure we use relative path
                    if(!(path.charAt(0) === '.')){
                        path = '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test' + path;
                    } else {
                        path = '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test' + path.substr(1);
                    }
                } else {
                    // path is very important, so if we don't get anything, have to treat this as a loose file and put it somewhere it can't collide with non-loose files
                    path = '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test/loose/';
                }
                try {
                    fs.mkdirsSync(path); 
                }
                catch(e) {
                    console.log(e); // mkdir probably failed
                }
                callback(null, path);
			},
			filename: (req, file, callback) => {
				//originalname is the uploaded file's name with extn
				callback(null, file.originalname);
			}
		})
	})
	.any();


// var anyUpload = upload.any();

// Deprecated.  Using /uploadFilesTest
// router.post('/upload', function (req, res) {
// 	console.log("Got POST at /upload");
//     if(!verifyHeaders(req.headers)) {
//         res.status(403).send();
//         return;
//     }
// 	anyUpload(req, res, function (err) {
// 		if (err instanceof multer.MulterError) {
// 			// A Multer error occurred when uploading.
// 			console.log("Multer Error", err);
// 			res.status(500).send("Multer Error: " + err);
// 		}
// 		else if (err) {
// 			// An unknown error occurred when uploading.
// 			console.log("Unknown error", err);
// 			res.status(500).send("Unknown Error: " + err);
// 		}
// 		else {
// 			// Everything went fine.
// 			console.log("OK");
// 			res.status(200).send("OK");
// 		}
// 	})
// })

// TODO: If incoming folder path has no identifying folder we need to make one based on the document ID which should be somewhere in the req.body? Or header "ID"
// I guess to support potential future possibilities we should also get and create a Type folder if we don't have one already
// Need Node.js: fs-extra
// Need to change port when testing and using this
router.post('/uploadFilesTest', function (req, res) {
	console.log("Got POST at /uploadFilesTest");
    if(!verifyHeaders(req.headers)) {
        console.log("🚀 ~ file: express_uploader.js:113 ~ UNABLE TO VERIFY req.headers:", req.headers)
        res.status(403).send();
        return;
    }

	uploadFiles(req, res, function (err) {
		// console.log(req.files, req.body);
		// console.log(req.files);
		// console.log(req.body);
		// console.log(req.body["filepath"]);
		if (err instanceof multer.MulterError) {
			// A Multer error occurred when uploading.
			console.log("Multer Error", err);
			res.status(500).send("Multer Error: " + err);
		}
		else if (err) {
			// An unknown error occurred when uploading.
			console.log("Unknown error", err);
			res.status(500).send("Unknown Error: " + err);
		}
		else {
			// Everything went fine.
			console.log("OK");
			res.status(200).send("OK");
		}
	})
})

// router.get('/extract', async function (req, res) {
// 	console.log("Got GET at /extract");

//     let filename = (req.headers["filename"]);
//     let path = "";
    
//     if(filename && typeof filename !== 'undefined'){ // "truthy": if non-null/undefined/NaN/empty/0/false, plus undeclared type check
//         path = './test/' + filename.split('.zip')[0]; // create, extract here

//         try {
//             console.log("Extracting to " + path);
//             fs.mkdirsSync(path); // create any necessary folders

//             let resolvedPath = resolve(path.split('.zip')[0]); // extract complains without this
//             await extract("./test/" + filename, { dir: resolvedPath })
//             console.log("Done");
//         }
//         catch(e) {
//             console.log("Exception during folder creation or extraction",e);
//         }
//     } else {
//         // ??
//         console.log("No filename");
//     }

//     res.status(500).send("Nope");
// });

/** Try to extract requested filename to self-named folder into download dir and without .zip ext */
router.post('/extract', express.urlencoded({extended: true}), function (req, res) {
	console.log(Date(Date.now()) + " Got POST at /extract",req.body);
    if(!verifyHeaders(req.headers)) {
        res.status(403).send();
        return;
    }

    let filename = (req.body["filename"]);
    let path = "";
    
    if(filename && typeof filename !== 'undefined'){ // "truthy": if non-null/undefined/NaN/empty/0/false, plus undeclared type check
        path = '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test/' + filename.split('.zip')[0]; // create, extract here
        zipPath = '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test/' + filename;

        // console.log("Extracting to " + resolve(path));
        let _filenames = [];
        fs.stat(zipPath, function(err, stat) {
            if(err == null) {
                // fs.mkdirsSync(path); // create any necessary folders (extract() does this too)

                let resolvedDestination = resolve(path); // extract() complains without resolving
                let resolvedSource = resolve(zipPath);

                const onEntry = function (entry) { // push each filename
                    if(entry.fileName.endsWith('/')) {
                        // this is a folder, not a file.
                    } else {
                        _filenames.push(entry.fileName);
                    }
                }

                // use onEntry to get filenames
                async function testExtract(source, directory) {
                    console.log("🚀 ~ file: express_uploader.js:202 ~ testExtract ~ source, directory:", source, directory)
                    let response = await extract(source, { dir: directory, onEntry });
                    console.log("TEST Exctract response",response);
                    return response;
                }

                testExtract(resolvedSource, resolvedDestination)
                    .then(response => {
                        console.log("TEST Exctract response",response);

                        // Delete extracted archive
                        fs.unlink(resolvedSource, () => {console.log("Deleted "+resolvedSource)});
                        // fs.unlink(file.path, function() { // callback
                        //     res.send ({
                        //         status: "200",
                        //         responseType: "string",
                        //         response: "success"
                        //     });     
                        // });

                        res.status(200).send({ filenames: _filenames });
                    })
                    .catch(error => {
                        console.error(Date(Date.now()) + " Problem with archive "+ resolvedSource,error);
                        // Delete problem archive
                        fs.unlink(resolvedSource, () => {console.log("Deleted problem archive "+resolvedSource)});
                        res.status(500).send(" Problem with archive: " + zipPath + ": " + error);
                    });

            } else if(err.code === 'ENOENT') {
                // file does not exist, skip
                console.error(Date(Date.now()) + " No file at "+resolve(zipPath));
                res.status(404).send("No file found: " + zipPath);
            } else {
                console.error(Date(Date.now()) + ' Some other error: ', err.code);
                res.status(500).send("Exception during folder creation or extraction");
            }
        })
    } else {
        console.log(Date(Date.now()) + " No filename given");
        res.status(400).send("No filename given");
    }

});


const fieldsTestUpload = upload.fields([{
	name: 'test'
}]);

router.post('/test', fieldsTestUpload, function (req, res, next) {
    if(!verifyHeaders(req.headers)) {
        res.status(403).send();
        return;
    }
	console.log(req.files['test'][0]);
	console.log(req.body);
	res.status(200).send("Probably OK");
})


router.post('/delete_file', express.urlencoded({extended: true}), function (req, res) {
	console.log(Date(Date.now()) + " Got POST at /delete_file");
    if(!verifyHeaders(req.headers)) {
        console.log("🚀 ~ file: express_uploader.js:266 ~ COULD NOT VERIFY HEADER req.headers:", req.headers)
        res.status(403).send();
        return;
    }

    let filename = (req.headers["filename"]);
    console.log("🚀 ~ file: express_uploader.js:271 ~ filename:", filename)
    let path = "";
    
    if(filename && typeof filename !== 'undefined'){ // "truthy": if non-null/undefined/NaN/empty/0/false, plus undeclared type check
        path = '/Users/georgeschaffer/code/nepa/backend/debug/media/data/nepaccess/test' + filename;

        console.log("Deleting from path:", path,"RESPOLVED PATH", resolve(path));

        fs.stat(path, function(err, stat) {
            if(err == null) {
                let resolvedDestination = resolve(path);

                // Delete file
                try {
                    fs.unlink(resolvedDestination, () => {console.log("Deleted "+resolvedDestination)});
    
                    res.status(200).send();
                } catch(error) {
                    console.error(Date(Date.now()) + " Problem with file "+ resolvedDestination,error);

                    res.status(500).send(" Problem with: " + resolvedDestination + ": " + error);
                };


            } else if(err.code === 'ENOENT') {
                // file does not exist, skip
                console.error(Date(Date.now()) + " No file at "+path);
                res.status(404).send("No file found: " + path);
            } else {
                console.error(Date(Date.now()) + ' Some other error: ', err.code);
                res.status(500).send("Exception: " + err.code);
            }
        })
    } else {
        console.log(Date(Date.now()) + " No filename given");
        res.status(400).send("No filename given");
    }

});

// router.post('/test3', upload.none(), function (req, res, next) {
//     console.log(req.body);
// })

// router.post('/test4', upload.single('test'), function (req, res) {
//     // req.file is the name of your file in the form above, here 'uploaded_file'
//     // req.body will hold the text fields, if there were any 
//     console.log("Got POST at /test");
//     console.log(req.file, req.body)
// });

app.use("/", router);

app.listen(port, () => console.log(`Upload handler listening at http://localhost:${port}`))