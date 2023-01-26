// Explanation: Set up to simply be put in the same folder as the one containing all of the downloadable files, 
// and set up for that folder to be named "test"

const express = require('express')
const multer =  require('multer') //https://github.com/expressjs/multer
const fs =      require('fs-extra')

const extract = require('extract-zip')
const resolve = require('path').resolve // to sort out extract-zip's life

const app = express()
const router = express.Router()
const port = 5678
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

// Take "filepath" field containing path and make that directory if it doesn't exist, then add file to it
let uploadFiles = multer({
		storage: multer.diskStorage({
			destination: (req, file, callback) => {
				console.log(req.files, req.body);
                let path = req.body["filepath"];
                if(path && typeof path !== 'undefined'){ // "truthy": if non-null/undefined/NaN/empty/0/false, plus undeclared type check
                    // Ensure we use relative path
                    if(!(path.charAt(0) === '.')){
                        path = './test' + path;
                    } else {
                        path = './test' + path.substr(1);
                    }
                } else {
                    // path is very important, so if we don't get anything, have to treat this as a loose file and put it somewhere it can't collide with non-loose files
                    path = './test/loose/';
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

router.post('/uploadFilesTest', function (req, res) {
	console.log("Got POST at /uploadFilesTest");
    if(!verifyHeaders(req.headers)) {
        res.status(403).send();
        return;
    }

	uploadFiles(req, res, function (err) {
		if (err instanceof multer.MulterError) {
			console.log("Multer Error", err);
			res.status(500).send("Multer Error: " + err);
		}
		else if (err) {
			console.log("Unknown error", err);
			res.status(500).send("Unknown Error: " + err);
		}
		else {
			console.log("OK");
			res.status(200).send("OK");
		}
	})
})

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
        path = './test/' + filename.split('.zip')[0]; // create, extract here
        zipPath = './test/' + filename;

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
                    let response = await extract(source, { dir: directory, onEntry });
                    return response;
                }

                testExtract(resolvedSource, resolvedDestination)
                    .then(response => {
                        console.log(Date(Date.now()) + " Done",_filenames);

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
        res.status(403).send();
        return;
    }

    let filename = (req.headers["filename"]);
    let path = "";
    
    if(filename && typeof filename !== 'undefined'){ // "truthy": if non-null/undefined/NaN/empty/0/false, plus undeclared type check
        path = './test' + filename;

        console.log("Deleting " + resolve(path));

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

app.use("/", router);

app.listen(port, () => console.log(`Upload handler listening at http://localhost:${port}`))