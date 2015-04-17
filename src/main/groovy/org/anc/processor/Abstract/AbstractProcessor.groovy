package org.anc.processor.Abstract

import org.anc.conf.AnnotationConfig
import org.anc.index.api.Index
import org.anc.index.core.IndexImpl
import org.anc.tool.api.IProcessor
import org.anc.tool.api.ProcessorException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xces.graf.io.dom.ResourceHeader

import javax.ws.rs.GET
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Response

/**
 * Created by danmccormack on 12/12/14.
 */
class AbstractProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProcessor)

    private static final Messages MESSAGES = new Messages()

    public List<String> Acceptable

    public IProcessor processor
    public ResourceHeader header
    public Index index

    void setAcceptable(List<String> accepted) {
        Acceptable = accepted
    }

    AbstractProcessor(List<String> passingAnnotations){
        setAcceptable(passingAnnotations);
        //TODO This path should not be hard coded.
        header = new ResourceHeader(new File("/var/corpora/MASC-3.0.0/resource-header.xml"))
        index = new IndexImpl().loadMasc3Index()
    }

    static def PennTreeBank = ["f.ptb", "f.ptbtok"] as List<String>

    static def PartOfSpeech = ["f.penn", "f.sentences", "f.biber", "f.c5", "f.c7", "f.cb",
                        "f.content", "f.event", "f.hepple", "f.logical",
                        "f.mpqa", "f.nc", "f.ne", "f.none",
                        "f.slate_coref", "f.vc", "f.s"] as List<String>

    static def FN_Antns = ["f.fn", "f.fntok"] as List<String>

    /**
     * Check if the annotations provided are acceptable for processing
     * @param antnArray - ArrayList<String> of annotations
     * @return A boolean response if the annotations passed in are acceptable for
     *  processing.
     */

    public boolean validAnnotations(List<String> selected) {
        def returnval = true
        if (selected.size() == 0)
            return false
        else {
            //It's a PTB_FN_Acceptable
            if (PennTreeBank.contains(selected[0])) {
                for (String annotation : selected) {
                    if (!PennTreeBank.contains(annotation)) {
                        returnval = false
                    }
                    if (!Acceptable.contains(annotation)){
                        returnval = false
                    }
                }
            }
            //Part of Speech
            else if(PartOfSpeech.contains(selected[0])){
                for (String annotation : selected){
                    if (!PartOfSpeech.contains(annotation)){
                        returnval = false
                    }
                    if (!Acceptable.contains(annotation)){
                        returnval = false
                    }
                }
            }
            //fn
            else if (FN_Antns.contains(selected[0])) {
                for (String annotation : selected) {
                    if (!FN_Antns.contains(annotation)) {
                        returnval = false
                    }
                    if (!Acceptable.contains(annotation)){
                        returnval = false
                    }
                }
            }
            else{
                returnval = false;
            }
        }

        return returnval
    }

    /**
     * Split the comma separated string into an ArrayList<String>
     * @param antnString - The comma separated string of selected annotations
     * @return An ArrayList<String> of the selected annotations
     */
    List<String> parseAnnotations(String antnString) {
        if (antnString == "") {
            return Acceptable.toList() as List<String>
        } else {
            // The collect closure will prepend the string 'f.' to every element in the
            // list.
            def retArray = antnString.split(',').collect { "f." + it.trim() }
            return retArray
        }
    }

    /**
     * Function called to process a document
     * @param annotations The list of annotations to process the doc with
     * @param docID The document ID
     * @return An error indicating an invalid parameter or the document processed with
     *         the annotations
     */
    @GET
    Response process(@QueryParam('annotations') String annotations,
                     @QueryParam('id') String docID) {
        if (annotations == null) {
            return Response.serverError().entity(MESSAGES.NO_ANNOTATIONS)
        }
        if (docID == null) {
            return Response.serverError().entity(MESSAGES.NO_DOC_ID)
        }

        logger.debug("Attempting to process {}", docID)



        def selectedAnnotations = parseAnnotations(annotations)
        System.out.println(selectedAnnotations)
        File inputFile = index.get(docID)
        if (inputFile == null) {
            logger.debug("No document with id {}", docID)
            return Response.serverError().entity(MESSAGES.INVALID_ID).build();
        }

        if (!validAnnotations(selectedAnnotations)) {
            logger.debug("Invalid annotations selected.")
            return Response.serverError().entity(MESSAGES.INVALID_TYPE).build()
        }

        // TODO The output file should be placed in a known location (e.g. /tmp/conll-processor) that
        // can be cleaned/deleted on startup to prevent filling the disk if the service crashes.

        File outputFile = File.createTempFile("processor-rs", "txt");

        try {
            processor.reset()
            processor.resourceHeader = header
            processor.annotationTypes = new AnnotationConfig(selectedAnnotations)
            processor.initialize()
            processor.process(inputFile, outputFile);
            return Response.ok(outputFile.text).build();
        }
        catch (ProcessorException e) {
            logger.error("Unable to process document.", e)
            return Response.status(500).entity(e.message).build()
        }
        finally {
            // Delete the temporary output file. This is best done in a finally block
            // so it gets deleted regardless of how execution exits the try block.
            if (!outputFile.delete()) {
                logger.error("Unable to delete temporary file {}", outputFile.path)
                // Hopefully we can delete the file when the service exits.
                outputFile.deleteOnExit();
            }
        }
    }
}
