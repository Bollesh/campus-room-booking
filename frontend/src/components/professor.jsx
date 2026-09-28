export default function professor(form, handleChange){
    return (
        <div className="professor-form">
          <div className="cul-sel">
            <p>Are you in a cultural department?</p>
            <select name="inCul" value={form.inCul} onChange={handleChange}>
              <option value="no">No</option>
              <option value="yes">Yes</option>
            </select>
          </div>
    
          <div className="club-head">
            <p>Club Head of:</p>
            <select name="clubHead" value={form.clubHead} onChange={handleChange}>
              <option value="none">None</option>
              <option value="drama">Drama Club</option>
              <option value="music">Music Club</option>
              <option value="tech">Tech Club</option>
              <option value="sports">Sports Club</option>
            </select>
          </div>
        </div>
      );
}