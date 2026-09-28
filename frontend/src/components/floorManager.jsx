export default function floorManager(form, setForm, handleChange){
    return (
        <div className="floor-manager-form">
            <p>Block Managed</p>
            <select name="block" value={form.block} onChange={handleChange}>
              <option value="">Select Block</option>
              <option value="A">Block A</option>
              <option value="B">Block B</option>
              <option value="C">Block C</option>
              <option value="D">Block D</option>
            </select>
        </div>
      );
}