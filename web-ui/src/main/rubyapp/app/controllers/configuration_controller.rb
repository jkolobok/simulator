require "rexml/document"
#Configuration controller is responsible for importing/exporting system configuration from/to xml files
#
class ConfigurationController < ApplicationController

  include ConfigurationHelper

# this magic method fixes file upload problems.
  protect_from_forgery :only => [:create, :update, :destroy]

  def export
    systems = System.find(:all)

    #doc - REXML document
    doc = systems_to_xml(systems)
    #todo add error handling

    response.headers["Content-Type"] = 'application/xml'
    render :xml => doc.to_s;
  end



#
#
#
  def import
    file_path = params[:upload].local_path
    file = File.new(file_path)
    doc = REXML::Document.new file
      import_xml(doc)
     render :template => "success"
  end
 
end
