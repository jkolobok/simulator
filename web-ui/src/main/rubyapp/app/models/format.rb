class Format < ActiveRecord::Base
  belongs_to :conversation
  belongs_to :format_type
  
  has_many :configurations

  validates_presence_of :format_type_id, :conversation_id
end
